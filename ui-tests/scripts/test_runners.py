"""Device-free regression checks: python3 -m unittest discover -s ui-tests/scripts -v."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest import mock
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
SCRIPTS = ROOT / "ui-tests/scripts"

# Every external device call is intercepted; these tests must never touch a device.
MOCK = """#!/usr/bin/env python3
import json, os, subprocess
from pathlib import Path
import sys
command = Path(sys.argv[0]).name
args = sys.argv[1:]
with open(os.environ['MOCK_LOG'], 'a') as log:
    log.write(json.dumps([command, *args]) + '\\n')
if command == 'adb':
    if 'get-state' in args:
        print('device')
    elif 'getprop' in args:
        print('1')
    elif 'clear' in args:
        sys.exit(int(os.environ.get('MOCK_CLEAR_STATUS', '0')))
elif command == 'maestro':
    failing = os.environ.get('MOCK_FAIL_FLOW', '') in args
    once = os.environ.get('MOCK_FAIL_ONCE_FLOW', '')
    if once in args:
        calls = [json.loads(line) for line in Path(os.environ['MOCK_LOG']).read_text().splitlines()]
        failing = sum(call[0] == 'maestro' and once in call for call in calls) == 1
    output = next((arg.split('=', 1)[1] for arg in args if arg.startswith('--output=')), None)
    if output and not os.environ.get('MOCK_NO_REPORT'):
        Path(output).write_text('<testsuite tests="1" failures="' + str(int(failing)) + '"/>')
    sys.exit(int(failing))
elif command == 'xcrun' and 'install' in args:
    sys.exit(int(os.environ.get('MOCK_INSTALL_STATUS', '0')))
elif command == 'xcrun' and 'keychain' in args:
    if os.environ.get('MOCK_KEYCHAIN_FAIL_ONCE'):
        calls = [json.loads(line) for line in Path(os.environ['MOCK_LOG']).read_text().splitlines()]
        if sum(call[0] == 'xcrun' and 'keychain' in call for call in calls) == 1:
            sys.exit(124)
    sys.exit(int(os.environ.get('MOCK_KEYCHAIN_STATUS', '0')))
elif command == 'xcrun' and 'list' in args:
    print('    iPhone 17 Pro UI Tests (mock-simulator) (Booted)')
elif command == 'firebase':
    sys.exit(subprocess.call(['bash', '-c', args[-1]]))
elif command == 'xcodebuild':
    if os.environ.get('MOCK_BUILD_CONFIG'):
        Path(os.environ['MOCK_BUILD_CONFIG']).write_text(Path('iosApp/iosApp/GoogleService-Info.plist').read_text())
    sys.exit(int(os.environ.get('MOCK_BUILD_STATUS', '99')))
"""


class RunnerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="ui-test-runners-")
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)
        self.log = self.directory / "commands.jsonl"
        for name in ("adb", "maestro", "xcrun", "firebase", "xcodebuild"):
            tool = self.directory / name
            tool.write_text(MOCK)
            tool.chmod(0o755)
        self.env = dict(os.environ, PATH=f"{self.directory}:{os.environ['PATH']}",
                        MOCK_LOG=str(self.log), SIMULATOR_ID="mock-simulator",
                        MAESTRO_FLOW_ATTEMPTS="1", MAESTRO_FLOW_TIMEOUT="8m",
                        MAESTRO_OUTPUT_DIR=str(self.directory / "reports"))
        # Avoid inheriting real CI/local runner configuration.
        for key in ("MOCK_FAIL_FLOW", "MOCK_INSTALL_STATUS", "MOCK_FAIL_ONCE_FLOW",
                    "MOCK_NO_REPORT", "UI_TEST_RUN_ID", "MAESTRO_FLOW_FILTER", "MOCK_CLEAR_STATUS",
                    "UI_TEST_APK", "UI_TEST_APP_PATH", "MOCK_BUILD_STATUS", "MOCK_BUILD_CONFIG",
                    "MOCK_KEYCHAIN_FAIL_ONCE", "MOCK_KEYCHAIN_STATUS"):
            self.env.pop(key, None)

    def run_suite(self, platform, shard, **env):
        result = subprocess.run(
            ["bash", str(SCRIPTS / f"run-{platform}-ui-tests.sh"), "--suites"],
            env=dict(self.env, SHARD=shard, **env), cwd=ROOT,
            capture_output=True, text=True, timeout=120)
        commands = [json.loads(line) for line in self.log.read_text().splitlines()] if self.log.exists() else []
        flows = [arg for entry in commands if entry[0] == "maestro"
                 for arg in entry[1:] if arg.endswith(".yml")]
        return result, commands, flows

    def expected_flows(self, platform, shard):
        free_boundary = 43 if platform == "ios" else 50
        paths = []
        if shard in ("all", "login"):
            paths += sorted((ROOT / "ui-tests/login").glob("*.yml"))
        if shard in ("all", "free", "free-a", "free-b"):
            paths += [p for p in sorted((ROOT / "ui-tests/after-login/free").glob("*.yml"))
                      if shard not in ("free-a", "free-b")
                      or (int(p.name.split("-")[0]) < free_boundary) == (shard == "free-a")]
        if shard in ("all", "premium"):
            paths += sorted((ROOT / "ui-tests/after-login/premium").glob("*.yml"))
        excluded = "platform: iOS" if platform == "android" else "platform: Android"
        # Only top-level configuration restricts a whole flow. A cross-platform
        # flow can contain conditional platform commands after the YAML separator.
        return [str(p.relative_to(ROOT)) for p in paths
                if not any(line.startswith(excluded)
                           for line in p.read_text().partition("\n---")[0].splitlines())]

    def test_inline_platform_commands_do_not_exclude_the_invite_flow(self):
        for platform in ("android", "ios"):
            with self.subTest(platform=platform):
                self.assertIn("ui-tests/after-login/free/12-invite.yml", self.expected_flows(platform, "free"))

    def test_free_shards_cover_every_flow_once_with_platform_specific_balancing(self):
        for platform in ("android", "ios"):
            a = self.expected_flows(platform, "free-a")
            b = self.expected_flows(platform, "free-b")
            self.assertEqual(set(a) & set(b), set())
            self.assertEqual(sorted(a + b), self.expected_flows(platform, "free"))
            for name in ("43-record-sort", "44-record-multi-select-delete"):
                path = f"ui-tests/after-login/free/{name}.yml"
                self.assertIn(path, b if platform == "ios" else a)

    def test_shards_and_platform_gates(self):
        for platform in ("android", "ios"):
            for shard in ("all", "login", "free", "free-a", "free-b", "premium"):
                with self.subTest(platform=platform, shard=shard):
                    self.log.unlink(missing_ok=True)
                    result, _, flows = self.run_suite(platform, shard)
                    self.assertEqual(result.returncode, 0, result.stderr)
                    self.assertEqual(flows, self.expected_flows(platform, shard))

    def test_failure_propagates_and_later_flows_still_run(self):
        failing = "ui-tests/after-login/premium/01-no-banner-ad.yml"
        for platform in ("android", "ios"):
            with self.subTest(platform=platform):
                self.log.unlink(missing_ok=True)
                result, _, flows = self.run_suite(platform, "premium", MOCK_FAIL_FLOW=failing,
                                                  MAESTRO_FLOW_ATTEMPTS="2")
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(flows.count(failing), 2)
                self.assertEqual(flows[-1], "ui-tests/after-login/premium/10-members.yml")

    def test_invalid_shard_fails_before_device_calls(self):
        for platform in ("android", "ios"):
            with self.subTest(platform=platform):
                result, commands, _ = self.run_suite(platform, "typo")
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(commands, [])

    def test_ios_failed_install_aborts_before_maestro(self):
        result, commands, flows = self.run_suite("ios", "premium", MOCK_INSTALL_STATUS="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(flows, [])
        self.assertEqual(sum(call[0] == "xcrun" and "install" in call for call in commands), 2)

    def test_ios_keychain_timeout_retries_reset_before_running_flows(self):
        result, commands, flows = self.run_suite(
            "ios", "free-a", MOCK_KEYCHAIN_FAIL_ONCE="1", MAESTRO_FLOW_FILTER="/01-record-digit-entry\\.yml$")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(flows, ["ui-tests/after-login/free/01-record-digit-entry.yml"])
        self.assertEqual(sum(call[0] == "xcrun" and "keychain" in call for call in commands), 2)
        self.assertEqual(sum(call[0] == "xcrun" and "install" in call for call in commands), 1)
        self.assertIn("keychain reset failed (attempt 1/2)", result.stderr)
        # Recovery must reset auth before the only install and before Maestro starts.
        steps = [call[2] for call in commands if call[0] == "xcrun"]
        self.assertEqual(steps, ["terminate", "uninstall", "keychain"] * 2 + ["install"])

    def test_ios_persistent_keychain_timeout_aborts_after_two_attempts(self):
        result, commands, flows = self.run_suite("ios", "free-a", MOCK_KEYCHAIN_STATUS="124")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(flows, [])
        self.assertEqual(sum(call[0] == "xcrun" and "keychain" in call for call in commands), 2)
        self.assertFalse(any("install" in call for call in commands))
        report = self.directory / "reports/after-login-free/setup/report.xml"
        self.assertEqual(ET.parse(report).getroot().get("errors"), "1")

    def test_prebuilt_apps_skip_build_and_survive_firebase_reentry_and_retry(self):
        flow = "ui-tests/after-login/premium/10-members.yml"
        for platform, variable in (("android", "UI_TEST_APK"), ("ios", "UI_TEST_APP_PATH")):
            with self.subTest(platform=platform):
                self.log.unlink(missing_ok=True)
                artifact = self.directory / f"{platform} prebuilt app"
                if platform == "ios":
                    artifact.mkdir()
                    (artifact / "Info.plist").write_text("mock plist")
                else:
                    artifact.write_text("mock APK")
                result = subprocess.run(
                    ["bash", str(SCRIPTS / f"run-{platform}-ui-tests.sh")], cwd=ROOT,
                    env=dict(self.env, **{variable: str(artifact)}, SHARD="premium",
                             MAESTRO_FLOW_FILTER="/10-members\\.yml$", MOCK_FAIL_ONCE_FLOW=flow,
                             MAESTRO_FLOW_ATTEMPTS="2"),
                    capture_output=True, text=True, timeout=30)
                self.assertEqual(result.returncode, 0, result.stderr)
                commands = [json.loads(line) for line in self.log.read_text().splitlines()]
                self.assertFalse(any(call[0] == "xcodebuild" for call in commands))
                self.assertEqual(sum(call[0] == "firebase" for call in commands), 1)
                installs = [call for call in commands if "install" in call]
                self.assertTrue(installs)
                self.assertTrue(all(call[-1] == str(artifact) for call in installs))
                # iOS resets/reinstalls for retry; Android clears the already-installed app.
                self.assertEqual(len(installs), 2 if platform == "ios" else 1)
                self.assertEqual(sum(call[0] == "maestro" for call in commands), 2)

    def test_missing_prebuilt_app_fails_without_building_or_running_tests(self):
        for platform, variable in (("android", "UI_TEST_APK"), ("ios", "UI_TEST_APP_PATH")):
            with self.subTest(platform=platform):
                self.log.unlink(missing_ok=True)
                result = subprocess.run(
                    ["bash", str(SCRIPTS / f"run-{platform}-ui-tests.sh")], cwd=ROOT,
                    env=dict(self.env, **{variable: str(self.directory / "missing")}),
                    capture_output=True, text=True, timeout=10)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("Prebuilt", result.stderr)
                commands = [json.loads(line) for line in self.log.read_text().splitlines()] if self.log.exists() else []
                self.assertFalse(any(call[0] in ("xcodebuild", "maestro", "firebase") for call in commands))
                self.assertFalse(any("install" in call for call in commands))

    def test_ios_build_uses_test_config_without_a_device_and_restores_original(self):
        # A miniature checkout prevents this build-helper test touching local configs.
        checkout = self.directory / "checkout"
        scripts = checkout / "ui-tests/scripts"
        scripts.mkdir(parents=True)
        for name in ("build-ios-ui-test-app.sh", "runner-common.sh"):
            shutil.copyfile(SCRIPTS / name, scripts / name)
        fixture = checkout / "ui-tests/config/GoogleService-Info.plist"
        fixture.parent.mkdir()
        fixture.write_text("test Firebase config")
        service = checkout / "iosApp/iosApp/GoogleService-Info.plist"
        service.parent.mkdir(parents=True)
        observed = self.directory / "observed-config"
        for original in (None, "original config"):
            for status in (0, 7):
                with self.subTest(original=original, status=status):
                    self.log.unlink(missing_ok=True)
                    if original is not None:
                        service.write_text(original)
                    result = subprocess.run(
                        ["bash", str(scripts / "build-ios-ui-test-app.sh")],
                        env=dict(self.env, MOCK_BUILD_STATUS=str(status), MOCK_BUILD_CONFIG=str(observed)),
                        capture_output=True, text=True, timeout=10)
                    self.assertEqual(result.returncode, status, result.stderr)
                    self.assertEqual(observed.read_text(), fixture.read_text())
                    if original is None:
                        self.assertFalse(service.exists())
                    else:
                        self.assertEqual(service.read_text(), original)
                    commands = [json.loads(line) for line in self.log.read_text().splitlines()]
                    self.assertEqual(len(commands), 1)
                    build = commands[0]
                    self.assertEqual(build[0], "xcodebuild")
                    self.assertIn("generic/platform=iOS Simulator", build)
                    self.assertIn("APP_BUNDLE_ID=com.kevlina.budgetplus", build)
                    self.assertIn("SWIFT_ACTIVE_COMPILATION_CONDITIONS=$(inherited) UI_TEST", build)
                    service.unlink(missing_ok=True)

    def test_android_setup_failure_is_reported_without_claiming_an_empty_selection(self):
        result, _, flows = self.run_suite("android", "premium", MOCK_CLEAR_STATUS="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(flows, [])
        report = self.directory / "reports/after-login-premium/setup/report.xml"
        self.assertEqual(ET.parse(report).getroot().get("errors"), "1")
        self.assertFalse((self.directory / "reports/selection/report.xml").exists())

    def test_crash_cannot_reuse_old_passing_report(self):
        for platform in ("android", "ios"):
            with self.subTest(platform=platform):
                self.log.unlink(missing_ok=True)
                report = self.directory / platform / "after-login-premium/01-no-banner-ad/report.xml"
                report.parent.mkdir(parents=True)
                report.write_text('<testsuite tests="1" failures="0"/>')
                result, _, _ = self.run_suite(
                    platform, "premium", MAESTRO_OUTPUT_DIR=str(self.directory / platform),
                    MOCK_FAIL_FLOW="ui-tests/after-login/premium/01-no-banner-ad.yml")
                self.assertNotEqual(result.returncode, 0)
                self.assertGreater(int(ET.parse(report).getroot().get("failures", "0")), 0)

    def test_missing_report_is_an_explicit_junit_error(self):
        result, _, _ = self.run_suite("ios", "premium", MOCK_NO_REPORT="1")
        self.assertNotEqual(result.returncode, 0)
        reports = list((self.directory / "reports").rglob("report.xml"))
        self.assertEqual(len(reports), 10)
        self.assertTrue(all(ET.parse(path).getroot().get("errors") == "1" for path in reports))

    def test_recovered_retry_preserves_both_attempts_without_duplicate_reports(self):
        failing = "ui-tests/after-login/premium/01-no-banner-ad.yml"
        for platform in ("android", "ios"):
            with self.subTest(platform=platform):
                self.log.unlink(missing_ok=True)
                output = self.directory / platform
                result, commands, flows = self.run_suite(
                    platform, "premium", MOCK_FAIL_ONCE_FLOW=failing,
                    MAESTRO_OUTPUT_DIR=str(output), MAESTRO_FLOW_ATTEMPTS="2")
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(flows.count(failing), 2)
                flow_dir = output / "after-login-premium/01-no-banner-ad"
                metadata = json.loads((flow_dir / "result.json").read_text())
                self.assertEqual(metadata["attempt"], 2)
                self.assertTrue(metadata["passedAfterRetry"])
                attempts = sorted(flow_dir.glob("attempts/*/*/maestro.xml"))
                self.assertEqual(len(attempts), 2)
                self.assertEqual([ET.parse(path).getroot().get("failures") for path in attempts], ["1", "0"])
                self.assertEqual(len(list(output.rglob("report.xml"))), 10)
                if platform == "android":
                    clears = [call for call in commands if "clear" in call]
                    self.assertEqual(len(clears), 2)  # Initial suite + clean retry.

    def test_invalid_attempt_count_fails_before_device_calls(self):
        for platform in ("android", "ios"):
            result, commands, _ = self.run_suite(platform, "premium", MAESTRO_FLOW_ATTEMPTS="0")
            self.assertNotEqual(result.returncode, 0)
            self.assertEqual(commands, [])

    def test_flow_filter_runs_only_selected_smoke_tests(self):
        for platform in ("android", "ios"):
            with self.subTest(platform=platform):
                self.log.unlink(missing_ok=True)
                result, _, flows = self.run_suite(platform, "all", MAESTRO_FLOW_FILTER="/10-members\\.yml$")
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(flows, ["ui-tests/after-login/premium/10-members.yml"])

    def test_empty_filter_selection_is_failure_not_green(self):
        for platform in ("android", "ios"):
            with self.subTest(platform=platform):
                self.log.unlink(missing_ok=True)
                result, _, flows = self.run_suite(platform, "premium", MAESTRO_FLOW_FILTER="no-such-flow")
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(flows, [])
                report = self.directory / "reports/selection/report.xml"
                self.assertEqual(ET.parse(report).getroot().get("errors"), "1")

    def test_service_config_restored_on_success_and_failure(self):
        for had_original in (False, True):
            for exit_code in (0, 7):
                with self.subTest(had_original=had_original, exit_code=exit_code):
                    service = self.directory / "service config.json"
                    fixture = self.directory / "test.json"
                    fixture.write_text("test config")
                    if had_original:
                        service.write_text("local config (not release config)")
                    result = subprocess.run(
                        ["bash", "-euc", 'source "$1"; use_test_service_file "$2" "$3"; '
                         '[[ $(cat "$2") == "test config" ]]; exit "$4"',
                         "test", str(SCRIPTS / "runner-common.sh"), str(service), str(fixture), str(exit_code)],
                        capture_output=True, text=True, timeout=10)
                    self.assertEqual(result.returncode, exit_code, result.stderr)
                    if had_original:
                        self.assertEqual(service.read_text(), "local config (not release config)")
                        service.unlink()
                    else:
                        self.assertFalse(service.exists())
                    self.assertEqual(list(self.directory.glob("*.ui-test.*")), [])

    def test_service_backup_is_not_bundled_as_an_app_resource(self):
        resources = self.directory / "app resources"
        resources.mkdir()
        service = resources / "GoogleService-Info.plist"
        service.write_text("original")
        fixture = self.directory / "test.plist"
        fixture.write_text("fixture")
        result = subprocess.run(
            ["bash", "-euc", 'source "$1"; use_test_service_file "$2" "$3"; '
             '[[ "$BACKUP_SERVICE_FILE" != "${2%/*}/"* ]]; [[ -f "$BACKUP_SERVICE_FILE" ]]',
             "test", str(SCRIPTS / "runner-common.sh"), str(service), str(fixture)],
            env=dict(os.environ, TMPDIR=str(self.directory)), capture_output=True, text=True, timeout=10)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(service.read_text(), "original")
        self.assertEqual(list(resources.iterdir()), [service])
        self.assertEqual(list(self.directory.glob("budgetplus-service.*")), [])

    def test_shell_syntax(self):
        for script in SCRIPTS.glob("*.sh"):
            with self.subTest(script=script.name):
                subprocess.run(["bash", "-n", str(script)], check=True)


class SummaryTests(unittest.TestCase):
    def summary(self, xml, attempts=None):
        with tempfile.TemporaryDirectory(prefix="ui-test-summary-") as temp:
            directory = Path(temp)
            if xml is not None:
                (directory / "report.xml").write_text(xml)
            if attempts:
                (directory / "result.json").write_text(json.dumps({"attempt": attempts}))
            output = directory / "summary.md"
            subprocess.run(["bash", str(SCRIPTS / "junit-summary.sh"), "test", temp],
                           env=dict(os.environ, GITHUB_STEP_SUMMARY=str(output)), check=True)
            return output.read_text()

    def test_multiline_nested_suite_counts(self):
        summary = self.summary('''<testsuites tests="3"><testsuite tests="3" failures="1">
          <testsuite\n tests="1" failures="0"/>
          <testsuite tests="2" failures="1"/></testsuite></testsuites>''')
        self.assertIn("1 failing** (of 3 tests", summary)

    def test_missing_malformed_and_empty_never_report_green(self):
        for xml in (None, "<broken", '<testsuite tests="0"/>', '<testsuite tests="1" skipped="1"/>'):
            with self.subTest(xml=xml):
                self.assertNotIn("all 0 tests passing", self.summary(xml))
                self.assertNotIn("Result: :white_check_mark:", self.summary(xml))

    def test_passing_suite(self):
        self.assertIn("all 2 tests passing", self.summary('<testsuite tests="2" failures="0"/>'))

    def test_retry_health_is_visible(self):
        summary = self.summary('<testsuite tests="1" failures="0"/>', attempts=2)
        self.assertIn("pass after retry", summary)
        self.assertIn("1 retried flows, 1 recovered after retry", summary)


class AttemptTests(unittest.TestCase):
    def test_failed_cli_cleans_up_children_even_after_console_closes(self):
        import run_maestro

        with tempfile.TemporaryDirectory(prefix="ui-test-child-cleanup-") as temp:
            process = mock.Mock(stdout=iter([]))
            process.wait.return_value = 1
            argv = ["run_maestro.py", "--output-dir", temp, "--suite", "suite/flow", "--", "maestro"]
            with mock.patch.object(sys, "argv", argv), \
                    mock.patch.object(run_maestro.subprocess, "Popen", return_value=process), \
                    mock.patch.object(run_maestro, "terminate") as terminate, \
                    mock.patch.object(sys, "stdout"), mock.patch.object(sys, "stderr"):
                self.assertEqual(run_maestro.main(), 1)
                terminate.assert_called_once_with(process)

    def attempt(self, code, timeout="5s"):
        temp = tempfile.TemporaryDirectory(prefix="ui-test-attempt-")
        self.addCleanup(temp.cleanup)
        output = Path(temp.name)
        self.attempt_output = output
        command = [sys.executable, str(SCRIPTS / "run_maestro.py"),
                   "--output-dir", temp.name, "--suite", "suite/flow", "--timeout", timeout,
                   "--", sys.executable, "-c", code]
        result = subprocess.run(command, capture_output=True, text=True, timeout=10)
        report = ET.parse(output / "suite/flow/report.xml").getroot()
        metadata = json.loads((output / "suite/flow/result.json").read_text())
        return result, report, metadata

    def test_timeout_produces_failure_report(self):
        result, report, metadata = self.attempt("import time; time.sleep(30)", timeout="0.1s")
        self.assertEqual(result.returncode, 124)
        self.assertEqual(report.get("errors"), "1")
        self.assertIn("timeout", metadata["diagnostic"])

    def test_missing_malformed_empty_and_skipped_reports_cannot_pass(self):
        for xml in (None, "<broken", '<testsuite tests="0"/>', '<testsuite tests="1" skipped="1"/>'):
            with self.subTest(xml=xml):
                code = "pass" if xml is None else (
                    "from pathlib import Path; import sys; "
                    "p = next(a.split('=', 1)[1] for a in sys.argv if a.startswith('--output=')); "
                    f"Path(p).write_text({xml!r})")
                result, report, _ = self.attempt(code)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(report.get("errors"), "1")

    def test_nonzero_exit_with_passing_report_becomes_runner_error(self):
        code = ("from pathlib import Path; import sys; "
                "p = next(a.split('=', 1)[1] for a in sys.argv if a.startswith('--output=')); "
                "Path(p).write_text('<testsuite tests=\"1\" failures=\"0\"/>'); sys.exit(7)")
        result, report, metadata = self.attempt(code)
        self.assertEqual(result.returncode, 7)
        self.assertEqual(report.get("errors"), "1")
        self.assertIn("despite a passing report", metadata["diagnostic"])

    def test_console_is_preserved_even_if_maestro_crashes_at_startup(self):
        result, report, metadata = self.attempt("print('driver startup failed', flush=True); raise SystemExit(1)")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(report.get("errors"), "1")
        self.assertIn("driver startup failed", result.stdout)
        console = next(self.attempt_output.rglob("console.log"))
        self.assertIn("driver startup failed", console.read_text())

    def test_failing_report_cannot_be_hidden_by_zero_exit(self):
        code = ("from pathlib import Path; import sys; "
                "p = next(a.split('=', 1)[1] for a in sys.argv if a.startswith('--output=')); "
                "Path(p).write_text('<testsuite tests=\"1\" failures=\"1\"/>')")
        result, report, _ = self.attempt(code)
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(report.get("failures"), "1")


class DeadlineTests(unittest.TestCase):
    def test_timeout_and_exit_code_propagation(self):
        for code, timeout, expected in (("raise SystemExit(7)", "5s", 7),
                                        ("import time; time.sleep(30)", "0.1s", 124)):
            with self.subTest(expected=expected):
                result = subprocess.run([sys.executable, str(SCRIPTS / "run_bounded.py"),
                                         timeout, sys.executable, "-c", code],
                                        capture_output=True, text=True, timeout=10)
                self.assertEqual(result.returncode, expected, result.stderr)


if __name__ == "__main__":
    unittest.main()
