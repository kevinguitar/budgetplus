# Budget+ — UI Test Coverage

[Maestro](https://maestro.mobile.dev/) flows exercise the app against local Firebase
Auth and Firestore emulators. All applicable flows run on every CI PR.

| Platform | Login | Free | Premium | CI device |
|---|---:|---:|---:|---|
| Android | 5 | 36 | 10 | API 34, `google_apis`, x86_64 |
| iOS | 5 | 36 | 10 | iPhone 17 Pro simulator; first available iPhone as fallback |

Counts describe implemented coverage, not the result of a particular run.

## Test environment

- **App ID:** `com.kevlina.budgetplus`.
- **Firebase:** Auth on port `9099`, Firestore on `8080`; Android uses `adb reverse`.
- **Build modes:** Android `uiTest`; iOS `UI_TEST`. Config fixtures live in `config/`.
- **Authentication:** sign-in buttons use anonymous emulator auth, not real Google/Apple
  sign-in. Saved-account lookup and tutorial overlays are disabled.
- **Setup:** `common/setup-login.yml` signs in if needed, creates `UI Test Book` if
  needed, and verifies Record (`AC`). It does not reset all persisted preferences.
- **Premium:** `common/seed-premium.yml` opens the test-only
  `budgetplus://uiTestPremium` link, verifies the banner area is absent, and dismisses
  the unlock snackbar.

## Coverage

Flow names below correspond to `.yml` files in each suite directory.

### Login — `login/`

| Flow | Checks |
|---|---|
| `01-auth-screen` | Welcome screen and sign-in controls. |
| `02-google-signin-to-welcome` | Google button reaches onboarding. |
| `03-create-book` | Create `UI Test Book` and reach Record. |
| `04-create-book-validation` | Empty name is rejected; valid name succeeds. |
| `05-back-on-welcome-logs-out` | Android only: Back from onboarding returns to Auth. |
| `06-apple-signin-to-welcome` | iOS only: Apple button reaches onboarding. |

### Free — `after-login/free/`

| Area | Flows | Checks |
|---|---|---|
| Calculator | `01-record-digit-entry`, `02-record-decimal`, `03-record-arithmetic` | Digits, reset, decimals, arithmetic. |
| Add records | `04-add-expense`, `05-add-income` | Save and find the expected type, category, amount, and unique note in Search. |
| Validation | `06-empty-category-guard`, `07-empty-price-guard` | Missing category/price shows the expected snackbar. |
| Notifications | `09-notification-permission` | Grant permission if prompted after adding a record; prompt is optional. |
| Books | `10-book-dropdown`, `11-book-limit-paywall`, `12-invite` | Book menu, free-tier book limit, native invitation share sheet. |
| Categories | `20-category-add`, `21-category-duplicate`, `22-category-unsaved-guard` | Save a category, reject duplicates, confirm leaving unsaved changes. |
| Overview | `30-overview-basics`, `31-overview-mode-drill`, `32-overview-export` | Totals, period/type controls, category drill-down, CSV confirmation (cancelled). |
| Records | `40-record-edit`, `41-record-duplicate`, `42-record-delete`, `43-record-sort`, `44-record-multi-select-delete` | Edit, duplicate, delete, sort, and delete two selected records while leaving a third. |
| Search | `50-search-basics`, `51-search-period-gate` | Search controls; Last Month allowed, Last 6 Months paywalled. |
| Members | `60-settings-members`, `61-settings-rename-user` | Owner shown; renamed user verified in Members. |
| Book settings | `62-settings-rename-book`, `63-settings-book-currency` | Change and restore book name/currency. |
| Preferences | `64-settings-vibration`, `68-allow-members-edit` | Toggle both ways, verify persistence after re-entry, restore original state. |
| Premium gates | `65-settings-hide-ads-gate`, `66-settings-batch-gate` | Free users reach the paywall route. |
| Color tones | `67-color-tones` | Save Warm Tones of Dusk, reopen to verify, restore Milk Tea. |
| Ads | `70-banner-ad` | Stable `Banner Ad Area` exists regardless of SDK fill/loading. |
| Destructive actions | `90-settings-delete-book`, `91-settings-logout` | Delete book returns to onboarding and reprovisions; logout returns to Auth last. |

### Premium — `after-login/premium/`

Each flow runs login setup and premium seeding before its assertions.

| Flow | Checks |
|---|---|
| `01-no-banner-ad` | Banner area absent. |
| `02-no-interstitial` | Add seven records without an interstitial; banner remains absent. |
| `03-create-additional-book` | Create a second book without a paywall, then switch back. |
| `04-overview-custom-period` | Custom date-range picker opens without an upsell. |
| `05-search-premium-periods` | Last 6 Months selectable without a paywall. |
| `06-preferred-currency` | Change preferred currency to Euro, then restore. |
| `07-premium-color-tones` | Save Barbie and Ken, reopen to verify, restore Milk Tea. |
| `08-batch-record` | Batch Record opens with frequency/date controls, not a paywall. |
| `09-hide-ads-hidden` | Batch Record remains visible; Hide Ads is absent. |
| `10-members` | Members dialog shows Owner. |

### Limits

- RevenueCat initialization is skipped. Gate flows verify the real Unlock Premium
  route through a test-only header/Back control, not SDK paywall content or purchases.
- Category rename/delete/reorder, the free custom-period upsell, the free preferred-
  currency gate, calculator-button/chart-mode dropdowns, and account deletion have
  no dedicated flows.
- The internal Push Notifications admin tool is not covered.

## CI execution

Android and iOS build in parallel. Each platform builds its test app once, then its
UI shards download and install the same binary without compiling.

| Shard | Selection |
|---|---|
| `login` | Platform-applicable login flows |
| `free-a` | Free flows numbered below 50 |
| `free-b` | Free flows numbered 50 and above |
| `premium` | All premium flows |

- Each shard has an isolated device and Firebase emulators; flows run sequentially.
  Android cold-boots with snapshots disabled. Runners enforce platform exclusions.
- Build jobs have a 90-minute limit; UI jobs have 100 minutes. `final-status`
  requires both builds and both UI workflows to pass.
- Only emulator-backed test apps are uploaded, with one-day retention; production
  and staging apps are excluded. The iOS `.app` is tarred to preserve permissions
  and symlinks. Artifacts are downloadable, not private storage. After expiry,
  rerun the full workflow to rebuild them.

Workflow definitions: [CI](../.github/workflows/ci.yml),
[Android build](../.github/workflows/android-ci.yml),
[iOS build](../.github/workflows/ios-ci.yml),
[Android UI tests](../.github/workflows/android-ui-tests.yml),
[iOS UI tests](../.github/workflows/ios-ui-tests.yml).

## Running locally

Run commands from the repository root. Install Maestro, Firebase CLI, a JDK, and
the platform tools: Android SDK command-line tools (`ANDROID_HOME`) or Xcode.

### Android

```sh
./ui-tests/scripts/setup-local-android-emulator.sh
EMULATOR_AVD=budgetplus_ci_api34 SHARD=free-a \
  ./ui-tests/scripts/run-android-ui-tests.sh
```

The setup uses port 5554 and chooses arm64 on Apple Silicon, x86_64 otherwise.
Keep `EMULATOR_AVD` set so recovery restarts the same AVD.

### iOS

```sh
./ui-tests/scripts/setup-local-ios-simulator.sh
./ui-tests/scripts/run-ios-ui-tests.sh "iPhone 17 Pro UI Tests" free-a
```

Use a dedicated simulator: the runner resets its keychain, and an installed Stage
app can intercept the shared `budgetplus://` scheme.

Both runners also accept `free` (whole free suite) and `all` (default). Local runs
build with the test Firebase config and restore the original config afterward.
Set `UI_TEST_APK` (Android) or `UI_TEST_APP_PATH` (iOS `.app`) to reuse a prebuilt
test app; an explicit missing path fails rather than falling back to a build.
The standalone iOS builder is `scripts/build-ios-ui-test-app.sh`.

### Targeted local reliability smoke run

Use a Bash regex over flow paths and a separate output directory. Empty selections
fail; CI leaves the filter unset.

```sh
MAESTRO_FLOW_FILTER='/(04-add-expense|67-color-tones)\.yml$' \
MAESTRO_FLOW_ATTEMPTS=1 MAESTRO_OUTPUT_DIR=build/maestro/ios-smoke \
  ./ui-tests/scripts/run-ios-ui-tests.sh "iPhone 17 Pro UI Tests" free
```

### Runner regression checks

```sh
python3 -m unittest discover -s ui-tests/scripts -v
maestro check-syntax ui-tests/login/03-create-book.yml
```

CI runs the device-free Python checks on both operating systems before building.
They cover selection, failure propagation, retries, deadlines, config restoration,
prebuilt apps, and reporting—not actual UI behavior.

## Retries and diagnostics

- `MAESTRO_FLOW_ATTEMPTS`: attempts per flow, default `2`; use `1` to disable retries.
- `MAESTRO_FLOW_TIMEOUT`: deadline per attempt, default `8m`. Device/setup commands
  have separate bounded deadlines.
- Retries clear Android app data or uninstall/reset keychain/reinstall on iOS.
  iOS login flows also reset before each flow because auth survives `clearState`.
- Reports live under `build/maestro/{android,ios}/`. The final `report.xml` is
  authoritative; `attempts/<run-id>/<attempt>/` preserves raw reports, console logs,
  screenshots, hierarchies, and result metadata. Missing/invalid reports fail closed.
- Actions publishes JUnit checks and a summary distinguishing first-attempt passes
  from passes after retry. Diagnostic artifacts are retained for seven days.

**Before changing a failing flow:** download the failing shard's artifact, inspect
the failing and preceding screenshots, read its `maestro.log`, and check the screen
hierarchy. An assertion message alone does not distinguish a slow screen from a
missed tap, blocking dialog, or unavailable emulator.

## Maintaining flows

Shared helpers live in `common/`; emulator fixtures in `config/`; runners in `scripts/`.
Prefer existing setup, navigation, record, picker, and cleanup subflows over copying them.

### Assertions and selectors

- Match visible text or accessibility `contentDescription` using Maestro `text:`;
  runs use English. Prefer these over positional taps or Compose test tags.
- Useful test semantics include `bottom_nav_add`, `bottom_nav_history`,
  `overview_mode_toggle`, `record_cell_<index>`, `record_amount_<value>`,
  `record_data_<type>_<category>_<price>_<name>`, and state-bearing settings rows.
- `UiTestFlags` gates test affordances: persistent, tap-to-dismiss accessible
  snackbars; deterministic color-tone controls; and the paywall loader's Back control.
  State labels observe real data rather than bypassing mutations.
- Verify saved data and persisted settings, not just dialog closure. Establish
  starting state, assert the result before cleanup, and restore changed preferences.
  Flows must work alone, in a shard, and after a retry.

### Cross-platform pitfalls

- Use `return-to-record.yml` for Back/tab navigation and
  `dismiss-share-sheet.yml` for native sharing UI. Avoid system Back on iOS;
  shared Compose dialogs may need a scrim tap.
- Use Enter or a neutral label tap to dismiss keyboards. Dismiss persistent
  snackbars before tapping controls they could cover.
- Wait for actionable controls, not just screen titles: category-editor Add animates
  in; currency changes have a commit snackbar; iOS settings switches need the native
  switch inside the state-bearing row.
- Use `ensure-all-records-mode.yml` before record-list operations and the color-tone
  helpers to reveal/advance the carousel and restore Milk Tea through real saves.
- Guard flaky taps with bounded retries and verify the destination. An enabled child
  icon does not prove its parent is actionable.
- Do not set optional subflow parameter defaults in config-level `env`; Maestro can
  overwrite caller values. Use an undefined-value fallback expression instead.
