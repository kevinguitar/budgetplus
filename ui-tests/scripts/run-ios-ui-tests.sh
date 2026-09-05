#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
SERVICE_FILE="$ROOT_DIR/iosApp/iosApp/GoogleService-Info.plist"
TEST_SERVICE_FILE="$ROOT_DIR/ui-tests/config/GoogleService-Info.plist"
BACKUP_SERVICE_FILE="$ROOT_DIR/iosApp/iosApp/GoogleService-Info.plist.bak"
SIMULATOR_NAME=${1:-iPhone 17}
SIMULATOR_ID=$(xcrun simctl list devices available | awk -v name="$SIMULATOR_NAME" 'index($0, name " (") && /\(Booted\)/ { id = $(NF - 1); gsub(/[()]/, "", id); print id; exit }')

if [[ -z "$SIMULATOR_ID" ]]; then
  printf 'No booted simulator named %s found\n' "$SIMULATOR_NAME" >&2
  exit 1
fi

restore_service_file() {
  local result=$?
  trap - EXIT
  set +e

  if [[ -f "$BACKUP_SERVICE_FILE" ]]; then
    cp -p "$BACKUP_SERVICE_FILE" "$SERVICE_FILE"
    local restore_result=$?
    rm -f "$BACKUP_SERVICE_FILE"

    if ((restore_result != 0)); then
      printf 'Failed to restore %s from %s\n' "$SERVICE_FILE" "$BACKUP_SERVICE_FILE" >&2
      result=$restore_result
    fi
  else
    # No original config was present (e.g. CI), so just remove the swapped-in test config.
    rm -f "$SERVICE_FILE"
  fi

  exit "$result"
}

if [[ -f "$SERVICE_FILE" ]]; then
  cp -p "$SERVICE_FILE" "$BACKUP_SERVICE_FILE"
fi

trap restore_service_file EXIT
cp "$TEST_SERVICE_FILE" "$SERVICE_FILE"

cd "$ROOT_DIR"
xcodebuild build \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination "platform=iOS Simulator,id=$SIMULATOR_ID" \
  -derivedDataPath build/ui-tests \
  -quiet \
  CODE_SIGN_IDENTITY=- \
  ARCHS=arm64 \
  ONLY_ACTIVE_ARCH=YES \
  APP_BUNDLE_ID=com.kevlina.budgetplus \
  SWIFT_ACTIVE_COMPILATION_CONDITIONS='$(inherited) UI_TEST'

APP_PATH="build/ui-tests/Build/Products/Debug-iphonesimulator/BudgetPlus.app"

# Prefer the maestro on PATH; fall back to the default install location (CI installs it there).
MAESTRO_BIN=$(command -v maestro || echo "$HOME/.maestro/bin/maestro")

# When MAESTRO_OUTPUT_DIR is set (e.g. in CI), emit a per-suite JUnit XML report (so
# failures surface in the GitHub Actions run summary / PR checks via a test reporter) plus
# the debug output/screenshots for artifacts.
maestro_test() {
  local suite_name="$1"
  shift
  if [[ -n "${MAESTRO_OUTPUT_DIR:-}" ]]; then
    local out="$MAESTRO_OUTPUT_DIR/$suite_name"
    mkdir -p "$out"
    "$MAESTRO_BIN" test --udid "$SIMULATOR_ID" \
      --test-output-dir="$out" \
      --debug-output="$out" \
      --format=junit \
      --output="$out/report.xml" \
      "$@"
  else
    "$MAESTRO_BIN" test --udid "$SIMULATOR_ID" "$@"
  fi
}

# Force the simulator UI (and the app) to English so tests can match English strings.
xcrun simctl spawn "$SIMULATOR_ID" defaults write -g AppleLanguages '("en-US")'
xcrun simctl spawn "$SIMULATOR_ID" defaults write -g AppleLocale "en_US"

# Reinstalls the app from a fully reset state (fresh keychain wipes the persisted
# Firebase auth session, which launchApp:clearState does not clear on iOS).
reset_app() {
  xcrun simctl terminate "$SIMULATOR_ID" com.kevlina.budgetplus >/dev/null 2>&1 || true
  xcrun simctl uninstall "$SIMULATOR_ID" com.kevlina.budgetplus >/dev/null 2>&1 || true
  xcrun simctl keychain "$SIMULATOR_ID" reset >/dev/null 2>&1 || true
  xcrun simctl install "$SIMULATOR_ID" "$APP_PATH"
}

run_suites() {
  # Track failures explicitly instead of relying on set -e propagating out of this
  # function (it runs as a firebase emulators:exec child, where a non-final maestro
  # failure would otherwise be swallowed and the suite reported as green).
  local suite_result=0

  # login: each flow needs a truly unauthenticated start, so reset the keychain before
  # every flow (iOS persists Firebase auth in the keychain across clearState).
  for flow in ui-tests/login/*.yml; do
    # Honor per-flow platform gating (running a single file bypasses Maestro's own gate).
    if grep -q '^platform: Android' "$flow"; then
      continue
    fi
    reset_app
    maestro_test "login/$(basename "$flow" .yml)" "$flow" || suite_result=1
  done

  reset_app
  maestro_test after-login-free ui-tests/after-login/free || suite_result=1

  reset_app
  maestro_test after-login-premium ui-tests/after-login/premium || suite_result=1

  return "$suite_result"
}

export SIMULATOR_ID APP_PATH MAESTRO_BIN
export -f maestro_test reset_app run_suites

firebase --config ui-tests/config/firebase.json --project budgetplus-ui-tests \
  emulators:exec --only auth,firestore run_suites
