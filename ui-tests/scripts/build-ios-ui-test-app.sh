#!/usr/bin/env bash

# Build once without booting a simulator; CI shards install the same archived app.
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
source "$ROOT_DIR/ui-tests/scripts/runner-common.sh"
cd "$ROOT_DIR"

use_test_service_file "$ROOT_DIR/iosApp/iosApp/GoogleService-Info.plist" \
  "$ROOT_DIR/ui-tests/config/GoogleService-Info.plist"

xcodebuild build \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath build/ui-tests \
  CODE_SIGN_IDENTITY=- \
  ARCHS=arm64 \
  ONLY_ACTIVE_ARCH=YES \
  APP_BUNDLE_ID=com.kevlina.budgetplus \
  SWIFT_ACTIVE_COMPILATION_CONDITIONS='$(inherited) UI_TEST' \
  COMPILER_INDEX_STORE_ENABLE=NO \
  DEBUG_INFORMATION_FORMAT=dwarf \
  -skipPackagePluginValidation \
  -quiet
