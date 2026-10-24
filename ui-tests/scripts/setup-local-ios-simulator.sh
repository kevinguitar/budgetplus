#!/usr/bin/env bash

# Creates (if missing) and boots a local iOS simulator that mirrors the CI device, so local
# UI-test runs reproduce CI as closely as possible.
#
# CI (.github/workflows/ios-ui-tests.yml) boots an "iPhone 17 Pro" simulator (falling back
# to the first available iPhone if that model isn't installed on the runner image).
#
# After this script prints "Simulator ready", run the suite with:
#   ./ui-tests/scripts/run-ios-ui-tests.sh "iPhone 17 Pro"
#
# Requires Xcode + command line tools (xcrun/simctl).

set -euo pipefail

DEVICE_NAME="${1:-iPhone 17 Pro}"

if ! xcrun simctl help >/dev/null 2>&1; then
  echo "xcrun simctl not available. Install Xcode and its command line tools." >&2
  exit 1
fi

# Find an existing simulator with this exact name, else create one from the matching
# device type + the newest installed iOS runtime.
SIM_ID=$(xcrun simctl list devices available -j \
  | jq -r --arg n "$DEVICE_NAME" '[.devices[][] | select(.isAvailable == true and .name == $n)][0].udid // empty')

if [[ -z "$SIM_ID" ]]; then
  echo "No simulator named '$DEVICE_NAME'; creating one..."
  DEVICE_TYPE=$(xcrun simctl list devicetypes -j \
    | jq -r --arg n "$DEVICE_NAME" '[.devicetypes[] | select(.name == $n)][0].identifier // empty')
  if [[ -z "$DEVICE_TYPE" ]]; then
    echo "Device type '$DEVICE_NAME' is not installed in this Xcode." >&2
    echo "Install it via Xcode > Settings > Components, or pass an available name, e.g.:" >&2
    echo "  ./ui-tests/scripts/setup-local-ios-simulator.sh \"iPhone 16 Pro\"" >&2
    exit 1
  fi
  RUNTIME=$(xcrun simctl list runtimes -j \
    | jq -r '[.runtimes[] | select(.isAvailable == true and (.identifier | test("iOS")))] | sort_by(.version | split(".") | map(tonumber)) | last.identifier // empty')
  if [[ -z "$RUNTIME" ]]; then
    echo "No available iOS runtime. Install one in Xcode > Settings > Components." >&2
    exit 1
  fi
  SIM_ID=$(xcrun simctl create "$DEVICE_NAME" "$DEVICE_TYPE" "$RUNTIME")
  echo "Created simulator '$DEVICE_NAME' ($SIM_ID)."
fi

STATE=$(xcrun simctl list devices -j | jq -r --arg id "$SIM_ID" '.devices[][] | select(.udid == $id) | .state')
if [[ "$STATE" != "Booted" ]]; then
  echo "Booting simulator '$DEVICE_NAME'..."
  xcrun simctl boot "$SIM_ID"
  xcrun simctl bootstatus "$SIM_ID" -b
fi

# Match the runner: force English so tests can match English strings.
xcrun simctl spawn "$SIM_ID" defaults write -g AppleLanguages '("en-US")' || true
xcrun simctl spawn "$SIM_ID" defaults write -g AppleLocale "en_US" || true

open -a Simulator || true

echo "Simulator ready. Run: ./ui-tests/scripts/run-ios-ui-tests.sh \"$DEVICE_NAME\""
