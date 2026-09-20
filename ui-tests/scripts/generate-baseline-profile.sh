#!/usr/bin/env bash

# CI wrapper for Baseline Profile generation. Runs inside the
# reactivecircus/android-emulator-runner `script:` step (so a booted emulator is
# already available on emulator-5554). android-emulator-runner runs each inline
# `script:` line as its own `/usr/bin/sh -c`, so this committed script is invoked
# once with bash to keep pipefail + functions working, mirroring
# ui-tests/scripts/run-android-ci.sh.
#
# It boots the Firebase auth/firestore emulators, points the app at them via adb
# reverse tunnels, then runs `:androidApp:generateBaselineProfile`. The generator
# drives the `nonMinifiedRelease` app variant (is_ui_test=true) through anonymous
# sign-in + book creation and writes the merged profile into
# androidApp/src/main/generated/baselineProfiles/baseline-prof.txt.

set -uo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
cd "$ROOT_DIR"

adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
adb shell settings put global hide_error_dialogs 1

# The app routes Firebase to the emulators via 127.0.0.1; forward those ports from
# the device to the host so the auth + firestore emulators are reachable.
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080

# Run the profile generation inside the Firebase emulator environment.
# emulators:exec runs its command via /bin/sh, so re-invoke gradle directly.
firebase --config ui-tests/config/firebase.json --project budgetplus-ui-tests \
  emulators:exec --only auth,firestore \
  "./gradlew :androidApp:generateBaselineProfile"
