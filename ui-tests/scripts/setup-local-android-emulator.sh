#!/usr/bin/env bash

# Creates (if missing) and boots a local Android emulator that mirrors the CI device, so
# local UI-test runs reproduce CI as closely as possible.
#
# CI (.github/workflows/android-ui-tests.yml) runs:
#   api-level 34, target google_apis, arch x86_64 (or arm64-v8a on Apple Silicon locally).
#
# After this script prints "Emulator ready", run the suite with:
#   ./ui-tests/scripts/run-android-ui-tests.sh
#
# Requires the Android SDK cmdline-tools (sdkmanager/avdmanager) and $ANDROID_HOME set.

set -euo pipefail
ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
source "$ROOT_DIR/ui-tests/scripts/runner-common.sh"

API_LEVEL=34
TARGET=google_apis
# Match the host arch: CI uses x86_64; Apple Silicon dev machines need arm64-v8a.
case "$(uname -m)" in
  arm64 | aarch64) ARCH=arm64-v8a ;;
  *) ARCH=x86_64 ;;
esac
PROFILE=pixel_6
AVD_NAME="budgetplus_ci_api${API_LEVEL}"
ADB_SERIAL="emulator-5554"
SYSTEM_IMAGE="system-images;android-${API_LEVEL};${TARGET};${ARCH}"

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
EMULATOR="$ANDROID_HOME/emulator/emulator"
ADB="$ANDROID_HOME/platform-tools/adb"

for tool in "$SDKMANAGER" "$AVDMANAGER" "$EMULATOR" "$ADB"; do
  if [[ ! -x "$tool" ]]; then
    echo "Required Android SDK tool not found: $tool" >&2
    echo "Install the Android SDK cmdline-tools and set ANDROID_HOME." >&2
    exit 1
  fi
done

echo "Ensuring system image: $SYSTEM_IMAGE"
# `yes` gets SIGPIPE when sdkmanager exits; preserve sdkmanager's status rather
# than turning a successful install into exit 141 under pipefail.
set +o pipefail
yes | "$SDKMANAGER" --install "$SYSTEM_IMAGE" >/dev/null
set -o pipefail

if ! "$AVDMANAGER" list avd -c | grep -qx "$AVD_NAME"; then
  echo "Creating AVD '$AVD_NAME' (profile $PROFILE)..."
  echo "no" | "$AVDMANAGER" create avd \
    --name "$AVD_NAME" \
    --package "$SYSTEM_IMAGE" \
    --device "$PROFILE" \
    --force
else
  echo "AVD '$AVD_NAME' already exists."
fi

# Never reuse a different AVD, or let a connected physical phone receive setup commands.
if run_bounded 10s "$ADB" -s "$ADB_SERIAL" get-state >/dev/null 2>&1; then
  RUNNING_AVD=$(run_bounded 10s "$ADB" -s "$ADB_SERIAL" emu avd name | tr -d '\r' | head -n 1)
  if [[ "$RUNNING_AVD" != "$AVD_NAME" ]]; then
    printf 'Port 5554 belongs to AVD %s. Stop it before booting %s.\n' "$RUNNING_AVD" "$AVD_NAME" >&2
    exit 1
  fi
  echo "Reusing AVD '$AVD_NAME'."
else
  echo "Booting emulator '$AVD_NAME'..."
  "$EMULATOR" -avd "$AVD_NAME" \
    -port 5554 \
    -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot \
    >/dev/null 2>&1 &
fi
wait_for_android_boot "$ADB" "$ADB_SERIAL"

# Match CI: disable animations + the stylus-handwriting tutorial that pops over inputs.
run_bounded 15s "$ADB" -s "$ADB_SERIAL" shell settings put global window_animation_scale 0 || true
run_bounded 15s "$ADB" -s "$ADB_SERIAL" shell settings put global transition_animation_scale 0 || true
run_bounded 15s "$ADB" -s "$ADB_SERIAL" shell settings put global animator_duration_scale 0 || true
run_bounded 15s "$ADB" -s "$ADB_SERIAL" shell settings put secure stylus_handwriting_enabled 0 || true
run_bounded 15s "$ADB" -s "$ADB_SERIAL" shell settings put secure stylus_handwriting_default_value 0 || true

echo "Emulator ready. Run: EMULATOR_AVD=$AVD_NAME ./ui-tests/scripts/run-android-ui-tests.sh"
