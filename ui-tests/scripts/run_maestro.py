"""Run one bounded Maestro attempt, preserving diagnostics and a canonical JUnit result."""

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import threading
import time
import uuid
import xml.etree.ElementTree as ET


def write_json(path, value):
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n")
    temporary.replace(path)


def failure_report(path, suite, message):
    root = ET.Element("testsuite", name=suite, tests="1", failures="0", errors="1", skipped="0")
    case = ET.SubElement(root, "testcase", name=suite, classname="maestro.runner")
    ET.SubElement(case, "error", message=message, type="RunnerError").text = message
    ET.ElementTree(root).write(path, encoding="utf-8", xml_declaration=True)


def report_counts(path):
    root = ET.parse(path).getroot()
    suites = [suite for suite in root.iter("testsuite") if not suite.findall("testsuite")]
    if not suites:
        raise ValueError("no testsuite elements")
    counts = [sum(int(suite.get(key, "0")) for suite in suites)
              for key in ("tests", "failures", "errors", "skipped")]
    if any(value < 0 for value in counts) or counts[0] == 0 or counts[0] == counts[3]:
        raise ValueError("empty or entirely skipped report")
    return counts


def duration(value):
    match = re.fullmatch(r"([0-9]+(?:\.[0-9]+)?)([smh]?)", value)
    if not match:
        raise argparse.ArgumentTypeError("expected a positive duration, e.g. 480, 30s, 8m")
    seconds = float(match[1]) * {"": 1, "s": 1, "m": 60, "h": 3600}[match[2]]
    if seconds <= 0:
        raise argparse.ArgumentTypeError("timeout must be positive")
    return seconds


def terminate(process):
    # Maestro can leave Java/ADB children behind; kill the attempt's entire process group.
    try:
        os.killpg(process.pid, signal.SIGTERM)
        process.wait(timeout=5)
    except subprocess.TimeoutExpired:
        pass
    except ProcessLookupError:
        return
    finally:
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
    process.wait()


def capture_console(process, path):
    with path.open("w") as console:
        for line in process.stdout:
            console.write(line)
            console.flush()
            try:
                sys.stdout.write(line)
                sys.stdout.flush()
            except BrokenPipeError:
                pass


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", required=True, type=Path)
    parser.add_argument("--suite", required=True)
    parser.add_argument("--timeout", type=duration, default=480)
    parser.add_argument("--error", help="record a setup failure without starting Maestro")
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    command = args.command[1:] if args.command[:1] == ["--"] else args.command
    if not command and not args.error:
        parser.error("a Maestro command or --error is required")
    suite = Path(args.suite)
    if suite.is_absolute() or ".." in suite.parts:
        parser.error("suite must be a relative path within the output directory")

    output = args.output_dir / suite
    output.mkdir(parents=True, exist_ok=True)
    run_id = os.environ.get("UI_TEST_RUN_ID", uuid.uuid4().hex)
    if not re.fullmatch(r"[a-zA-Z0-9_-]+", run_id):
        parser.error("UI_TEST_RUN_ID must contain only letters, numbers, hyphens, or underscores")
    result_path = output / "result.json"
    try:
        previous = json.loads(result_path.read_text())
    except (OSError, ValueError):
        previous = {}
    if not isinstance(previous, dict):
        previous = {}
    previous_attempt = previous.get("attempt", 0)
    if not isinstance(previous_attempt, int) or previous_attempt < 0:
        previous_attempt = 0
    attempt = previous_attempt + 1 if previous.get("runId") == run_id else 1
    attempt_dir = output / "attempts" / run_id / str(attempt)
    attempt_dir.mkdir(parents=True, exist_ok=True)
    raw_report = attempt_dir / "maestro.xml"
    canonical = output / "report.xml"
    message = args.error or "Maestro did not finish this attempt or did not produce a valid report"
    # Write before launch: even runner termination cannot leave a stale green report.
    failure_report(canonical, args.suite, message)
    result = dict(runId=run_id, suite=args.suite, attempt=attempt, status="running",
                  startedAt=datetime.now(timezone.utc).isoformat())
    write_json(result_path, result)
    started = time.monotonic()
    exit_code = 1
    process = None
    console_thread = None
    if not args.error:
        command += [f"--test-output-dir={attempt_dir}", f"--debug-output={attempt_dir}",
                    "--format=junit", f"--output={raw_report}"]
        print(f"Running {args.suite} (attempt {attempt}, timeout {args.timeout:g}s)", flush=True)
        try:
            process = subprocess.Popen(command, start_new_session=True, stdout=subprocess.PIPE,
                                       stderr=subprocess.STDOUT, text=True, errors="replace")
            console_thread = threading.Thread(target=capture_console,
                                              args=(process, attempt_dir / "console.log"), daemon=True)
            console_thread.start()
            exit_code = process.wait(timeout=args.timeout)
            if exit_code != 0:
                # Failed iOS driver startup can exit the CLI while xcodebuild/XCTest
                # keeps running. Stop those children before retrying on the same device.
                terminate(process)
        except subprocess.TimeoutExpired:
            terminate(process)
            exit_code = 124
            message = f"Maestro exceeded the {args.timeout:g}s attempt timeout"
        except OSError as error:
            message = f"Could not start Maestro: {error}"
        except KeyboardInterrupt:
            if process is not None:
                terminate(process)
            exit_code = 130
            message = "Maestro attempt interrupted"

        if console_thread is not None:
            console_thread.join(timeout=5)
            if console_thread.is_alive():
                # A child inherited stdout after the CLI exited. Do not leak that child
                # or wait forever for the console stream to close.
                terminate(process)
                console_thread.join(timeout=5)

        try:
            tests, failures, errors, skipped = report_counts(raw_report)
            if exit_code in (124, 130):
                # Preserve the infrastructure cause even if Maestro wrote a partial
                # failing report before the deadline/interruption.
                pass
            elif exit_code != 0 and failures + errors == 0:
                message = f"Maestro exited {exit_code} despite a passing report"
            else:
                shutil.copyfile(raw_report, canonical)
                message = ""  # The native report is authoritative unless its exit code disagrees.
                if failures + errors:
                    exit_code = exit_code or 1
        except (OSError, ValueError, ET.ParseError) as error:
            message = f"{message}; missing/invalid Maestro report: {error}"
            exit_code = exit_code or 1

    if message:
        failure_report(canonical, args.suite, message)
        print(f"::error::{args.suite}: {message}", file=sys.stderr)
    result.update(status="passed" if exit_code == 0 else "failed", exitCode=exit_code,
                  durationSeconds=round(time.monotonic() - started, 3),
                  passedAfterRetry=exit_code == 0 and attempt > 1,
                  diagnostic=message)
    write_json(attempt_dir / "result.json", result)
    write_json(result_path, result)
    return exit_code if 0 <= exit_code <= 255 else 1


if __name__ == "__main__":
    sys.exit(main())
