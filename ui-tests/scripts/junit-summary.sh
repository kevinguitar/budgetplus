#!/usr/bin/env bash

# Usage: junit-summary.sh <platform-label> <maestro-output-dir>
# Python's XML parser handles multiline attributes, entities, and testsuites roots.
set -euo pipefail

python3 - "${1:?platform label required}" "${2:?output dir required}" \
  "${GITHUB_STEP_SUMMARY:-/dev/stdout}" <<'PY'
from pathlib import Path
import json
import sys
import xml.etree.ElementTree as ET

platform, output_dir, summary_file = sys.argv[1:]
reports = sorted(Path(output_dir).rglob("report.xml"))
lines = [f"## {platform} UI test results", ""]
totals = [0, 0, 0, 0]
invalid = 0
first_attempt_passes = 0
retried_flows = []
recovered_flows = []

if not reports:
    lines += ["> :warning: No JUnit reports were produced (the suite may have failed to start).", ""]
else:
    lines += ["| Flow / Suite | Status | Attempts | Tests | Failures | Errors | Skipped |",
              "|---|---|---|---|---|---|---|"]
    for report in reports:
        name = str(report.parent.relative_to(output_dir)).replace("|", "&#124;")
        try:
            result = json.loads((report.parent / "result.json").read_text())
            attempts = int(result["attempt"])
        except (OSError, ValueError, KeyError, TypeError):
            attempts = None
        try:
            root = ET.parse(report).getroot()
            # Only count leaf suites; parent suites can repeat aggregate totals.
            suites = [suite for suite in root.iter("testsuite")
                      if not suite.findall("testsuite")]
            if not suites:
                raise ValueError("no testsuite elements")
            counts = [sum(int(suite.get(key, "0")) for suite in suites)
                      for key in ("tests", "failures", "errors", "skipped")]
        except (ET.ParseError, ValueError, OSError):
            invalid += 1
            lines.append(f"| {name} | :warning: invalid report | {attempts or '—'} | — | — | — | — |")
            continue
        totals = [a + b for a, b in zip(totals, counts)]
        status = ":x: FAIL" if counts[1] + counts[2] else (
            ":warning: skipped/empty" if counts[0] == counts[3] else ":white_check_mark: pass")
        if attempts and attempts > 1:
            retried_flows.append(name)
            if status == ":white_check_mark: pass":
                status = ":warning: pass after retry"
                recovered_flows.append(name)
        elif attempts == 1 and status == ":white_check_mark: pass":
            first_attempt_passes += 1
        lines.append(f"| {name} | {status} | {attempts or '—'} | " + " | ".join(map(str, counts)) + " |")
    tests, failures, errors, skipped = totals
    lines.append("")
    if invalid or tests == 0 or tests == skipped:
        lines.append(f"**Result: :warning: incomplete results** ({tests} tests, "
                     f"{failures + errors} failing, {skipped} skipped, {invalid} invalid reports).")
    elif failures + errors:
        lines.append(f"**Result: :x: {failures + errors} failing** (of {tests} tests, {skipped} skipped).")
    else:
        lines.append(f"**Result: :white_check_mark: all {tests} tests passing** ({skipped} skipped).")
    lines += ["", f"**Retry health:** {first_attempt_passes} first-attempt passes, "
              f"{len(retried_flows)} retried flows, {len(recovered_flows)} recovered after retry."]
    if recovered_flows:
        lines.append("Passed only after retry: " + ", ".join(f"`{name}`" for name in recovered_flows) + ".")
    lines.append("")

with open(summary_file, "a") as summary:
    summary.write("\n".join(lines) + "\n")
PY
