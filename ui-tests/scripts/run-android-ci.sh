#!/usr/bin/env bash

# CI wrapper for the Android UI tests. android-emulator-runner runs each line of
# an inline `script:` as its own `/usr/bin/sh -c`, so a heredoc plus `set -o
# pipefail` cannot be expressed inline (dash lacks `pipefail`). This committed
# script is invoked once with bash so everything shares a single shell.

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
cd "$ROOT_DIR"

OUTPUT_DIR="${MAESTRO_OUTPUT_DIR:-build/maestro/android}"
mkdir -p "$OUTPUT_DIR"

adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
adb shell input keyevent 82
adb shell settings put global hide_error_dialogs 1

# Do not abort on failure: capture diagnostics before exiting.
result=0
./ui-tests/scripts/run-android-ui-tests.sh || result=$?

adb exec-out screencap -p > "$OUTPUT_DIR/final-screen.png" || true
adb logcat -d > "$OUTPUT_DIR/logcat.txt" || true

exit $result
