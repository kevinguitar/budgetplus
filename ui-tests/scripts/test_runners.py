"""Device-free regression checks: python3 -m unittest discover -s ui-tests/scripts -v."""

import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[2]
SCRIPTS = ROOT / "ui-tests/scripts"

# Every external device call is intercepted; these tests must never touch a device.
MOCK = """#!/usr/bin/env python3
import json, os
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
elif command == 'maestro':
    if os.environ.get('MOCK_FAIL_FLOW', '') in args:
        sys.exit(1)
elif command == 'xcrun' and 'install' in args:
    sys.exit(int(os.environ.get('MOCK_INSTALL_STATUS', '0')))
"""


class RunnerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="ui-test-runners-")
        self.addCleanup(self.temp.cleanup)
        self.directory = Path(self.temp.name)
        self.log = self.directory / "commands.jsonl"
        for name in ("adb", "maestro", "xcrun"):
            tool = self.directory / name
            tool.write_text(MOCK)
            tool.chmod(0o755)
        self.env = dict(os.environ, PATH=f"{self.directory}:{os.environ['PATH']}",
                        MOCK_LOG=str(self.log), SIMULATOR_ID="mock-simulator",
                        MAESTRO_FLOW_ATTEMPTS="1")
        # Avoid inheriting real CI/local runner configuration.
        for key in ("MAESTRO_OUTPUT_DIR", "MOCK_FAIL_FLOW", "MOCK_INSTALL_STATUS"):
            self.env.pop(key, None)

    def run_suite(self, platform, shard, **env):
        result = subprocess.run(
            ["bash", str(SCRIPTS / f"run-{platform}-ui-tests.sh"), "--suites"],
            env=dict(self.env, SHARD=shard, **env), cwd=ROOT,
            capture_output=True, text=True, timeout=30)
        commands = [json.loads(line) for line in self.log.read_text().splitlines()] if self.log.exists() else []
        flows = [entry[-1] for entry in commands if entry[0] == "maestro"]
        return result, commands, flows

    def expected_flows(self, platform, shard):
        paths = []
        if shard in ("all", "login"):
            paths += sorted((ROOT / "ui-tests/login").glob("*.yml"))
        if shard in ("all", "free", "free-a", "free-b"):
            paths += [p for p in sorted((ROOT / "ui-tests/after-login/free").glob("*.yml"))
                      if shard not in ("free-a", "free-b")
                      or (int(p.name.split("-")[0]) < 50) == (shard == "free-a")]
        if shard in ("all", "premium"):
            paths += sorted((ROOT / "ui-tests/after-login/premium").glob("*.yml"))
        excluded = "platform: iOS" if platform == "android" else "platform: Android"
        return [str(p.relative_to(ROOT)) for p in paths if excluded not in p.read_text()]

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
        result, _, flows = self.run_suite("ios", "premium", MOCK_INSTALL_STATUS="1")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(flows, [])

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
                self.assertFalse(report.exists())

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

    def test_shell_syntax(self):
        for script in SCRIPTS.glob("*.sh"):
            with self.subTest(script=script.name):
                subprocess.run(["bash", "-n", str(script)], check=True)


class SummaryTests(unittest.TestCase):
    def summary(self, xml):
        with tempfile.TemporaryDirectory(prefix="ui-test-summary-") as temp:
            directory = Path(temp)
            if xml is not None:
                (directory / "report.xml").write_text(xml)
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


if __name__ == "__main__":
    unittest.main()
