#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
SERVICE_FILE="$ROOT_DIR/iosApp/iosApp/GoogleService-Info.plist"
TEST_SERVICE_FILE="$ROOT_DIR/ui-tests/config/GoogleService-Info.plist"
BACKUP_SERVICE_FILE="$ROOT_DIR/iosApp/iosApp/GoogleService-Info.plist.bak"
SIMULATOR_NAME=${1:-iPhone 17}
# Which suite(s) to run. CI shards the suites across parallel jobs so no single
# job approaches the 100-minute workflow ceiling (the full sequential suite is
# ~84 minutes of tests on top of a ~31-minute cold framework build). The free
# suite alone (~35 flows) overran the 100-minute job timeout, so it is split into
# two balanced sub-shards by flow-number range. Accepts:
#   login   – ui-tests/login/*.yml
#   free-a  – ui-tests/after-login/free flows numbered <= 43
#   free-b  – ui-tests/after-login/free flows numbered >= 50
#   free    – ui-tests/after-login/free (whole suite; used for local runs)
#   premium – ui-tests/after-login/premium
#   all     – every suite (default; used for local runs)
SHARD=${2:-all}
case "$SHARD" in
  login | free | free-a | free-b | premium | all) ;;
  *)
    printf 'Unknown shard %q (expected: login, free, free-a, free-b, premium, all)\n' "$SHARD" >&2
    exit 1
    ;;
esac
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

# Number of attempts per flow. UI flows are occasionally flaky on CI from transient
# simulator glitches that a clean re-run clears: a GPU/render blip that paints the whole
# app black while the view hierarchy stays intact, a dropped tap, an ad/network hiccup.
# Retry a failed flow (from a fully reset app state) before counting it as a real
# failure. Set MAESTRO_FLOW_ATTEMPTS=1 to disable retries.
MAESTRO_FLOW_ATTEMPTS=${MAESTRO_FLOW_ATTEMPTS:-2}

# Run one flow, retrying up to MAESTRO_FLOW_ATTEMPTS times. Before each retry the app is
# reset to a clean state (fresh install + keychain wipe) so the re-run does not inherit a
# wedged UI. The JUnit report of the final attempt overwrites the earlier one, so the
# reported result reflects the last (decisive) run — a flow that passes on retry is green.
maestro_test_retry() {
  local suite_name="$1"
  local attempt=1
  while true; do
    if maestro_test "$suite_name" "${@:2}"; then
      return 0
    fi
    if ((attempt >= MAESTRO_FLOW_ATTEMPTS)); then
      return 1
    fi
    echo "::warning::Flow $suite_name failed on attempt $attempt/$MAESTRO_FLOW_ATTEMPTS; resetting app and retrying." >&2
    attempt=$((attempt + 1))
    reset_app
  done
}

# Run every flow in a directory one at a time, each with the retry wrapper. Honors
# per-flow platform gating (running a single file bypasses Maestro's own gate). A single
# failing flow does not abort the rest of the suite; failures are surfaced via the return
# code. Emits per-flow JUnit reports (like the free-a/free-b sub-shards) so every flow
# still shows up individually in the run summary.
run_flow_dir() {
  local suite_prefix="$1"
  local dir="$2"
  local result=0
  for flow in $(ls "$dir"/*.yml | sort); do
    if grep -q '^platform: Android' "$flow"; then
      continue
    fi
    maestro_test_retry "$suite_prefix/$(basename "$flow" .yml)" "$flow" || result=1
  done
  return "$result"
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
  if [[ "$SHARD" == "login" || "$SHARD" == "all" ]]; then
    for flow in ui-tests/login/*.yml; do
      # Honor per-flow platform gating (running a single file bypasses Maestro's own gate).
      if grep -q '^platform: Android' "$flow"; then
        continue
      fi
      reset_app
      maestro_test_retry "login/$(basename "$flow" .yml)" "$flow" || suite_result=1
    done
  fi

  if [[ "$SHARD" == "free" || "$SHARD" == "all" ]]; then
    reset_app
    run_flow_dir after-login-free ui-tests/after-login/free || suite_result=1
  fi

  # Balanced sub-shards of the free suite (see SHARD docs): free-a runs flows
  # numbered <= 43, free-b runs flows numbered >= 50. Passing explicit files (in
  # sorted order) keeps Maestro's per-flow reporting while running only a subset.
  if [[ "$SHARD" == "free-a" || "$SHARD" == "free-b" ]]; then
    reset_app
    for flow in $(ls ui-tests/after-login/free/*.yml | sort); do
      num=$(basename "$flow" | cut -d- -f1)
      if [[ "$SHARD" == "free-a" && "$num" -le 43 ]] || [[ "$SHARD" == "free-b" && "$num" -ge 50 ]]; then
        maestro_test_retry "after-login-free/$(basename "$flow" .yml)" "$flow" || suite_result=1
      fi
    done
  fi

  if [[ "$SHARD" == "premium" || "$SHARD" == "all" ]]; then
    reset_app
    run_flow_dir after-login-premium ui-tests/after-login/premium || suite_result=1
  fi

  return "$suite_result"
}

export SIMULATOR_ID APP_PATH MAESTRO_BIN SHARD MAESTRO_FLOW_ATTEMPTS
export -f maestro_test maestro_test_retry run_flow_dir reset_app run_suites

firebase --config ui-tests/config/firebase.json --project budgetplus-ui-tests \
  emulators:exec --only auth,firestore run_suites
