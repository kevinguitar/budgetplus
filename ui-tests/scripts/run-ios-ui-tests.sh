#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
source "$ROOT_DIR/ui-tests/scripts/runner-common.sh"
SUITES_ONLY=false
if [[ "${1:-}" == "--suites" ]]; then
  SUITES_ONLY=true
  shift
fi
SIMULATOR_NAME=${1:-iPhone 17 Pro UI Tests}
# Which suite(s) to run. CI builds once, then shards the suites across parallel
# jobs using UI_TEST_APP_PATH to install the prebuilt app. The free suite alone
# (~35 flows) overran the original 100-minute job timeout, so it is split into
# two sub-shards by flow-number range. Accepts:
#   login   – ui-tests/login/*.yml
#   free-a  – ui-tests/after-login/free flows numbered < 42
#   free-b  – ui-tests/after-login/free flows numbered >= 42
#   free    – ui-tests/after-login/free (whole suite; used for local runs)
#   premium – ui-tests/after-login/premium
#   all     – every suite (default; used for local runs)
SHARD=${SHARD:-${2:-all}}
validate_shard "$SHARD"
MAESTRO_OUTPUT_DIR=${MAESTRO_OUTPUT_DIR:-build/maestro/ios}
initialize_maestro_run
cd "$ROOT_DIR"
if [[ "$SUITES_ONLY" == false ]]; then
  SIMULATOR_ID=$(xcrun simctl list devices available | awk -v name="$SIMULATOR_NAME" 'index($0, name " (") && /\(Booted\)/ { id = $(NF - 1); gsub(/[()]/, "", id); print id; exit }')

  if [[ -z "$SIMULATOR_ID" ]]; then
    printf 'No booted simulator named %s found\n' "$SIMULATOR_NAME" >&2
    exit 1
  fi

  if [[ -z "${UI_TEST_APP_PATH:-}" ]]; then
    bash "$ROOT_DIR/ui-tests/scripts/build-ios-ui-test-app.sh"
  elif [[ ! -d "$UI_TEST_APP_PATH" || ! -f "$UI_TEST_APP_PATH/Info.plist" ]]; then
    printf 'Prebuilt iOS app not found: %s\n' "$UI_TEST_APP_PATH" >&2
    exit 1
  fi
fi

APP_PATH=${UI_TEST_APP_PATH:-build/ui-tests/Build/Products/Debug-iphonesimulator/BudgetPlus.app}

# Prefer the maestro on PATH; fall back to the default install location (CI installs it there).
MAESTRO_BIN=$(command -v maestro || echo "$HOME/.maestro/bin/maestro")

# Keep each attempt's diagnostics; publish only the final canonical report to CI.
maestro_test() {
  local suite_name="$1"
  shift
  run_maestro_attempt "$suite_name" "$MAESTRO_BIN" test --udid "$SIMULATOR_ID" "$@"
}

# Number of attempts per flow. UI flows are occasionally flaky on CI from transient
# simulator glitches that a clean re-run clears: a GPU/render blip that paints the whole
# app black while the view hierarchy stays intact, a dropped tap, an ad/network hiccup.
# Retry a failed flow (from a fully reset app state) before counting it as a real
# failure. Set MAESTRO_FLOW_ATTEMPTS=1 to disable retries.

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
    if ! reset_app; then
      record_flow_failure "$suite_name" "Could not reset/install the app before retry" || true
      return 1
    fi
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
  local flow
  for flow in "$dir"/*.yml; do
    if grep -q '^platform: Android' "$flow"; then
      continue
    fi
    flow_is_selected "$flow" || continue
    selected_flows=$((selected_flows + 1))
    maestro_test_retry "$suite_prefix/$(basename "$flow" .yml)" "$flow" || result=1
  done
  return "$result"
}

# Reinstalls the app from a fully reset state (fresh keychain wipes the persisted
# Firebase auth session, which launchApp:clearState does not clear on iOS).
reset_app() {
  # A freshly booted simulator can briefly stall keychain services. Retry the
  # complete reset once, without relaxing command deadlines or reusing auth state.
  local attempt
  for attempt in 1 2; do
    echo "Resetting iOS app (attempt $attempt/2)"
    run_bounded 15s xcrun simctl terminate "$SIMULATOR_ID" com.kevlina.budgetplus >/dev/null 2>&1 || true
    run_bounded 30s xcrun simctl uninstall "$SIMULATOR_ID" com.kevlina.budgetplus >/dev/null 2>&1 || true
    echo "Resetting simulator keychain"
    if ! run_bounded 30s xcrun simctl keychain "$SIMULATOR_ID" reset; then
      echo "::warning::Simulator keychain reset failed (attempt $attempt/2)." >&2
      continue
    fi
    echo "Installing iOS test app"
    if run_bounded 60s xcrun simctl install "$SIMULATOR_ID" "$APP_PATH"; then
      return 0
    fi
    echo "::warning::Simulator app install failed (attempt $attempt/2)." >&2
  done
  return 1
}

reset_for_suite() {
  if ! reset_app; then
    record_flow_failure "$1" "Could not reset/install the app before the suite" || true
    return 1
  fi
}

run_suites() {
  # Track failures explicitly instead of relying on set -e propagating out of this
  # function (it runs as a firebase emulators:exec child, where a non-final maestro
  # failure would otherwise be swallowed and the suite reported as green).
  local suite_result=0
  local flow num
  local selected_flows=0

  # login: each flow needs a truly unauthenticated start, so reset the keychain before
  # every flow (iOS persists Firebase auth in the keychain across clearState).
  if [[ "$SHARD" == "login" || "$SHARD" == "all" ]]; then
    for flow in ui-tests/login/*.yml; do
      # Honor per-flow platform gating (running a single file bypasses Maestro's own gate).
      if grep -q '^platform: Android' "$flow"; then
        continue
      fi
      flow_is_selected "$flow" || continue
      selected_flows=$((selected_flows + 1))
      reset_for_suite "login/$(basename "$flow" .yml)" || return 1
      maestro_test_retry "login/$(basename "$flow" .yml)" "$flow" || suite_result=1
    done
  fi

  if [[ "$SHARD" == "free" || "$SHARD" == "all" ]]; then
    reset_for_suite after-login-free/setup || return 1
    run_flow_dir after-login-free ui-tests/after-login/free || suite_result=1
  fi

  # Split at 42 to balance measured in-flow work (~23 min per shard), rather
  # than overfitting variable XCTest startup time. Android keeps its <50 split.
  # Explicit files retain per-flow reporting without adding macOS jobs.
  if [[ "$SHARD" == "free-a" || "$SHARD" == "free-b" ]]; then
    reset_for_suite after-login-free/setup || return 1
    for flow in ui-tests/after-login/free/*.yml; do
      num=$(basename "$flow" | cut -d- -f1)
      num=$((10#$num))
      if [[ "$SHARD" == "free-a" && "$num" -lt 42 ]] || [[ "$SHARD" == "free-b" && "$num" -ge 42 ]]; then
        if grep -q '^platform: Android' "$flow"; then
          continue
        fi
        flow_is_selected "$flow" || continue
        selected_flows=$((selected_flows + 1))
        maestro_test_retry "after-login-free/$(basename "$flow" .yml)" "$flow" || suite_result=1
      fi
    done
  fi

  if [[ "$SHARD" == "premium" || "$SHARD" == "all" ]]; then
    reset_for_suite after-login-premium/setup || return 1
    run_flow_dir after-login-premium ui-tests/after-login/premium || suite_result=1
  fi

  if ((selected_flows == 0)); then
    record_flow_failure selection "No flows matched the requested shard/platform/filter" || true
    return 1
  fi
  return "$suite_result"
}

if [[ "$SUITES_ONLY" == true ]]; then
  run_suites
  exit $?
fi

# Force English before launching the app.
xcrun simctl spawn "$SIMULATOR_ID" defaults write -g AppleLanguages '("en-US")'
xcrun simctl spawn "$SIMULATOR_ID" defaults write -g AppleLocale "en_US"

export SIMULATOR_ID SHARD MAESTRO_FLOW_ATTEMPTS MAESTRO_FLOW_FILTER UI_TEST_APP_PATH

firebase --config ui-tests/config/firebase.json --project budgetplus-ui-tests \
  emulators:exec --only auth,firestore "bash ui-tests/scripts/run-ios-ui-tests.sh --suites"
