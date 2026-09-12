# Budget+ — Maestro UI Test Coverage

A full-regression UI test suite for the Budget+ KMP/Compose app, driven by
[Maestro](https://maestro.mobile.dev/). **Implemented and passing on both platforms:**

| Platform | Login | after-login/free | after-login/premium |
|---|---|---|---|
| Android (API 34, google_apis) | 5/5 | 35/35 | 10/10 |
| iOS (iPhone 17 Pro, iOS 26) | 5/5 | 35/35 | 10/10 |

All flows are enabled and run on every CI PR — there are no `disabled`/excluded flows.

The suites are split into three independent Maestro runs:

1. **`login`** — runs unauthenticated (fresh app state); exercises auth + onboarding.
2. **`after-login/free`** — assumes an authorized anonymous (free) user with a book, so
   launching lands directly on the **RecordScreen**.
3. **`after-login/premium`** — same, but each flow seeds `premium=true` first.

Everything runs against the Firebase **auth + firestore emulators** using the existing
`uiTest` build type (Android) / `UI_TEST` compilation condition (iOS).

---

## 1. Background — How the app decides what to show

Initial destination (`BookActivity` / iOS `NavigationInitActionProvider`):

| Condition | Destination |
|---|---|
| `authManager.userState == null` | `BookDest.Auth()` (login flow) |
| authed but `currentBookId == null` / no books | `BookDest.Welcome` (create/join book) |
| authed **and** has a book | **RecordScreen** (home) |

Test-mode facts:
- `UiTestEnvironment.enabled` (set from Android `R.bool.is_ui_test` = true in the
  `uiTest` flavor; iOS `#if UI_TEST`) routes Firebase to the emulators, makes the auth
  buttons call `signInAnonymouslyForUiTest()` (anonymous emulator sign-in), and disables
  the tutorial `Bubble` overlays.
- App id: `com.kevlina.budgetplus`. Emulator ports: **auth 9099**, **firestore 8080**.
- Premium is a server-side boolean `premium` field on the user's Firestore doc
  (`authManager.isPremium`), seeded via a test-only deeplink (see §6).

---

## 2. Directory structure

Each suite is its own Maestro run so they are fully independent, separately-reportable
pipelines.

```
ui-tests/
  common/                       # shared subflows (runFlow)
    setup-login.yml             # idempotent: sign in + create "UI Test Book" -> Record
    seed-premium.yml            # flips the user to premium (see §6)
    assert-on-record.yml        # waits for/asserts the calculator "AC" is visible
    return-to-record.yml        # robust cross-platform "go back to Record"
    ensure-logged-out.yml       # iOS: reset auth+book so the app shows the Auth screen
    ensure-all-records-mode.yml # forces Overview into All-Records mode before long-press
    open-category-records.yml   # Overview -> Records screen for a category
    dismiss-system-dialogs.yml  # optional taps for OS permission/Apple-ID dialogs
    add-record.yml              # adds one expense record (price 100) in a category
  login/                        # SUITE 1 — unauthenticated (each flow clearState: true)
    01-auth-screen.yml
    02-google-signin-to-welcome.yml
    03-create-book.yml
    04-create-book-validation.yml
    05-back-on-welcome-logs-out.yml   # platform: Android
    06-apple-signin-to-welcome.yml    # platform: iOS
  after-login/
    free/                       # SUITE 2 — anonymous (free) user (§4)
    premium/                    # SUITE 3 — each flow runs seed-premium first (§5)
  config/                       # emulator/firebase config (unchanged)
  scripts/
    run-android-ui-tests.sh
    run-ios-ui-tests.sh
```

### Achieving the "after login" precondition
Every `after-login` flow starts with **`common/setup-login.yml`** (`runFlow`), which is
idempotent: it launches, dismisses any OS dialog, then branches on the current screen —
signs in if on Auth, creates `UI Test Book` if on Welcome, and asserts landing on Record
(`AC`). Emulator + app state persist across flows within one `maestro test` run, so the
first flow provisions and the rest reuse it.

---

## 3. SUITE 1 — `login` (unauthenticated)

| ID | Flow | Assertions |
|---|---|---|
| L1 | `01-auth-screen` | Assert `Welcome to Budget+`, `Continue with Google`. |
| L2 | `02-google-signin-to-welcome` | Tap `Continue with Google` → Welcome: `Book Name`, placeholder `My accounting book`, `Go`, `You can create a new accounting book`. |
| L4 | `03-create-book` | Input `UI Test Book`, `Go` → Record (`AC`) + book name in top bar. |
| L5 | `04-create-book-validation` | Blank name → `Go` stays on Welcome; typing a name makes it land on Record. |
| L6 | `05-back-on-welcome-logs-out` | **Android-only.** Back on Welcome → Auth screen. (iOS has no equivalent gesture Maestro can trigger for the predictive-back logout handler.) |
| L3 | `06-apple-signin-to-welcome` | **iOS-only.** Tap `Continue with Apple` → Welcome. |

Notes:
- L1 also renders `Continue with Apple`; the Apple-specific Welcome assertion lives in the
  iOS-only L3.
- Platform gating is enforced by the runner scripts (Maestro's per-flow `platform:` field is
  not honored when a single file is invoked directly), so the Android runner skips
  `platform: iOS` flows and the iOS runner skips `platform: Android` flows.

---

## 4. SUITE 2 — `after-login/free`

The anonymous test user has no `premium` flag → treated as free. Flows are ordered by
filename; destructive flows (delete book, logout) run last.

### 4.1 Record screen — calculator & add-record
| ID | Flow | Key assertions |
|---|---|---|
| R1 | `01-record-digit-entry` | `7 8 9` → `789`; `AC` → `0`. |
| R2 | `02-record-decimal` | `1 . 5` → `1.5` (decimal button tapped by position — icon). |
| R3 | `03-record-arithmetic` | `2 × 3 =` → `6` (operators/equals are icons, tapped by position). |
| R4 | `04-add-expense` | Expense → `Food` → `120` → `OK` → price resets to `0`. |
| R5 | `05-add-income` | Income (placeholder `Where did you earn from?`) → `Salary` → `50` → `OK`. |
| R6 | `06-empty-category-guard` | Price, no category, `OK` → snackbar `Please choose a category`. |
| R7 | `07-empty-price-guard` | Category, price `0`, `OK` → snackbar `Please input the price`. |

### 4.2 Book selector & invite
| ID | Flow | Key assertions |
|---|---|---|
| B1 | `10-book-dropdown` | `Select book` → dropdown shows `UI Test Book` + `Create a New Book`. |
| B2 | `11-book-limit-paywall` | `Create a New Book` (free limit 1) → routes to Unlock Premium. |
| B3 | `12-invite` | `Invite` → OS share sheet (Record home leaves view). |

### 4.3 Categories
| ID | Flow | Key assertions |
|---|---|---|
| C1/C2 | `20-category-add` | `Edit` → `Edit Categories`; FAB `Add` → dialog `Category`, add `Groceries`, `Save` → snackbar `Category list has been saved`. |
| C5 | `21-category-duplicate` | Add existing name `Food` → snackbar `Category Food already exist`. |
| C6 | `22-category-unsaved-guard` | Make a change, tap back arrow → `Do you want to leave without saving the changes?` → `Confirm`. |

### 4.4 Overview / History
| ID | Flow | Key assertions |
|---|---|---|
| O1/O2/O3/O8 | `30-overview-basics` | `...'s Overview` title, `Total`/`Balance`, Expense/Income toggle, `1D`/`1W`/`1M`/`LM` pills. |
| O5/O6 | `31-overview-mode-drill` | Mode toggle → Group-by-categories → tap category → Records screen `<category>: <total>`. |
| O7 | `32-overview-export` | `Export csv` → confirm dialog `Do you want to export...` (entry asserted, then cancelled). |

### 4.5 Records list — edit, delete, duplicate, sort
| ID | Flow | Key assertions |
|---|---|---|
| E1/E2 | `40-record-edit` | Tap record → `Edit Record` dialog → change price → `Save` → snackbar `Record edited`. |
| E4 | `41-record-duplicate` | Long-press record → `Duplicate` → snackbar `Record duplicated`. |
| E3 | `42-record-delete` | Long-press → `Delete` → `Are you sure you want to delete the record?` → `Confirm` → snackbar `Record ... is deleted`. |
| E5 | `43-record-sort` | Records screen sort toggle `Sort by price` ↔ `Sort by date`. |

### 4.6 Search
| ID | Flow | Key assertions |
|---|---|---|
| S1/S2/S3 | `50-search-basics` | Overview `Search` FAB → `Search` screen, `Enter keyword`, filter pills `Expense`/`All Categories`. |
| S4 | `51-search-period-gate` | Period sheet: `Last Month` allowed; `Last 6 Months` routes free user to paywall. |

### 4.7 Settings
| ID | Flow | Key assertions |
|---|---|---|
| ST1/ST3 | `60-settings-members` | `...'s Settings`; `View Members` → `Members` dialog, `Owner` label. |
| ST2 | `61-settings-rename-user` | `Rename User` → `User Name` dialog → snackbar `Renamed to Tester`. |
| ST4 | `62-settings-rename-book` | `Rename Book` → snackbar `Renamed the book to ...` (renamed then restored). |
| ST5 | `63-settings-book-currency` | `Edit Book Currency` → `Book's Currency` picker → change to Euro then restore US Dollar. |
| ST9 | `64-settings-vibration` | Toggle `Input Vibration` switch. |
| ST12 | `65-settings-hide-ads-gate` | `Hide Ads` (free-only row) → routes to paywall. |
| ST13 | `66-settings-batch-gate` | `Batch Record` → routes free user to paywall. |
| ST8 | `67-color-tones` | `Color Tone Picker` → swipe carousel to free tone `Warm Tones of Dusk` → `Save`. |
| ST7 | `68-allow-members-edit` | Toggle `Allow Members to Edit` (owner-only switch). |
| ST15 | `90-settings-delete-book` | `Delete Book` → `Are you sure...` → `Confirm` → Welcome; re-provisions a book. (Destructive; near end.) |
| ST14 | `91-settings-logout` | `Logout` → Auth screen. (**Runs last.**) |

### 4.8 Ads
| ID | Flow | Key assertions |
|---|---|---|
| A1 | `70-banner-ad` | Banner ad area present for free users: `Test Ad` (Android test ad) **or** `No ads available` (iOS simulator). |

### 4.9 Notification permission
| ID | Flow | Key assertions |
|---|---|---|
| N1 | `09-notification-permission` | After first record, grant the OS notification dialog if shown (best-effort/optional). |

---

## 5. SUITE 3 — `after-login/premium`

Each flow runs `setup-login` then `seed-premium` (§6) so `isPremium=true`, asserting the
**unlocked** experience.

| ID | Flow | Key assertions |
|---|---|---|
| P1 | `01-no-banner-ad` | Banner area absent (`Test Ad`/`No ads available` both gone). |
| P2 | `02-no-interstitial` | Add 7 records; no interstitial; flow proceeds; still no banner. |
| P3 | `03-create-additional-book` | `Create a New Book` opens the create screen (no lock); create `Second Book` (`Copy categories from` + `Create`) → snackbar `Your book Second Book is created!`; switch back. |
| P4 | `04-overview-custom-period` | `Select Date` opens the date-range picker (`Select dates`); no `Unlock premium to set the period above one month` upsell. |
| P5 | `05-search-premium-periods` | `Last 6 Months` selectable without a paywall. |
| P6 | `06-preferred-currency` | `Edit Preferred Currency` opens the picker (not the paywall); change to Euro then restore. |
| P7 | `07-premium-color-tones` | Swipe to premium tone `Barbie and Ken` → `Save` succeeds (no paywall). |
| P8 | `08-batch-record` | `Batch Record` opens its screen (`Number of times:`, `Batch Frequency:`, `Start Date:`) — not the paywall. |
| P9 | `09-hide-ads-hidden` | Settings shows `Batch Record` but **not** `Hide Ads`. |
| P10 | `10-members` | `View Members` → `Members` dialog with `Owner`. |

### Paywall entry assertion
The Unlock Premium screen renders the RevenueCat paywall, which cannot transact in the
emulator (RevenueCat init is skipped under UI tests — see §7). Paywall **entry** is therefore
asserted by navigation (leaving the prior screen), not by paywall content.

---

## 6. Premium seeding (`common/seed-premium.yml`)

A UI-test-only deeplink `budgetplus://uiTestPremium` calls `authManager.markPremium(true)`
(only when `UiTestEnvironment.enabled`; handled in `BookViewModel.handleDeeplink`). The seed
subflow does `openLink budgetplus://uiTestPremium`, taps the iOS "Open in Budget+?"
confirmation if present, and confirms premium via the snackbar `Budget+ Premium unlocked!`
and the absence of the banner ad area. It is idempotent (already-premium users emit no
snackbar) and dismisses the (persistent) unlocked snackbar so it doesn't block later taps.

---

## 7. App changes made for testability (all test-mode-guarded)

- **`core/common/UiTestFlags`** — dependency-free flags (`enabled`, `persistentSnackbar`)
  set from `UiTestEnvironment.configure()` so lower-level modules can adapt for tests.
- **`BookViewModel`** — handles the `budgetplus://uiTestPremium` deeplink → `markPremium(true)`.
- **`core/ui/SnackbarHost`** — in UI-test mode, snackbars are shown `Indefinite` and their
  message is exposed via `contentDescription`, because Maestro could not otherwise reliably
  read the transient Material3 snackbar.
- **`RevenueCatInitializer`** — skips RevenueCat/StoreKit init under UI tests. This both
  avoids emulator purchase failures and, critically, stops the iOS "Sign in to Apple Account"
  system dialog that otherwise blocked automation.
- **`ColorToneCarousel`** — stable `contentDescription = "color_tone_pager"` on the pager so
  the tone carousel can be swiped reliably. It also exposes a **UI-test-only** deterministic
  "next" control (`contentDescription = "color_tone_next"`, only when `UiTestFlags.enabled`)
  that advances the pager one page via `animateScrollToPage`. The tone flows tap this instead
  of a fling `swipe`, which was too flaky in CI (the fling intermittently failed to advance a
  page).
- **`BudgetPlusApp` + `ui_test_network_security_config.xml`** — emulator host is `127.0.0.1`
  (with cleartext permitted), reachable from both the Android emulator (via `adb reverse`) and
  the iOS simulator.

There are still **no `Modifier.testTag`s**; JetBrains Compose Multiplatform for Android does
not expose `testTagsAsResourceId` here, so selectors use visible text and the accessible
`contentDescription`s above. Icon-only controls with no description (calculator operators/equals,
mic, the Overview mode toggle and period pencil) are tapped by position; the bottom-nav tabs
carry UI-test-only `contentDescription`s (`bottom_nav_add`, `bottom_nav_history`) so they can be
tapped by text instead, and the date-range picker is opened via the accessible `Select Date`
calendar icon.

---

## 8. Selector strategy & cross-platform notes

**Text/description based** (English locale forced): drive by the visible literals and the
icon `contentDescription`s (`Invite`, `Settings`, `Select book`, `Select Date`, `Search`,
`Export csv`, `Back`, `Save`, `Sort by price`/`Sort by date`, `color_tone_pager`,
`bottom_nav_add`, `bottom_nav_history`).

**Positional taps** (icon-only, no description): calculator operators/equals/decimal, the
Overview mode toggle, and dialog-scrim dismissals on iOS.

**Cross-platform robustness solutions baked into the flows/subflows:**
- **Keyboard dismissal**: `pressKey: Enter` (dialogs with an `onDone` action) or a neutral
  label tap, instead of `hideKeyboard` (unreliable on iOS SwiftUI).
- **Back navigation**: tap the top-bar `Back` arrow (contentDescription) via
  `return-to-record.yml`, instead of the system back gesture (iOS has none). Dialogs on iOS
  are dismissed by tapping the scrim.
- **Snackbars**: asserted with `extendedWaitUntil` on the **full** message text (the
  persistent-snackbar + contentDescription hooks make them catchable).
- **Overview mode persistence**: `ensure-all-records-mode.yml` probes/repairs the persisted
  Overview mode before long-press flows.
- **iOS auth persistence**: Firebase auth lives in the keychain across `clearState`, so the
  runner resets the keychain before each `login` flow, and `ensure-logged-out.yml` deletes the
  account when needed.
- **iOS deeplink**: taps the "Open in Budget+?" confirmation. `return-to-record.yml` also
  dismisses a late-reappearing "Open in Budget+?" dialog (from the premium seed) that could
  otherwise block the final Record assertion.
- **Color Tone Picker**: the picker's large "Color Preview" card can push the tone carousel
  below the fold, so the tone flows `scrollUntilVisible` the `color_tone_next` control, then
  advance the pager by tapping it (deterministic) rather than a fling `swipe`.
- **Deterministic taps with retry**: taps that occasionally don't register on iOS (the
  currency tiles in `63-settings-book-currency`, the Color-Tone-Picker settings row) are
  guarded by a `when: visible`/`notVisible` retry so a single missed tap doesn't fail the flow.

---

## 9. Runner scripts & execution

### `scripts/run-android-ui-tests.sh`
Swaps in the test `google-services.json`, `assembleUiTest`, installs, sets up `adb reverse`
for ports 9099/8080, disables the Gboard stylus-handwriting tutorial, then inside a single
`firebase emulators:exec` runs: `login` (looping files, skipping `platform: iOS`, with
`pm clear`), then `after-login/free`, then `after-login/premium` (each preceded by `pm clear`).
The production `google-services.json` is restored on exit. The `--suites` entry point
**propagates the real pass/fail exit code** (`exit $?`) so a failing flow fails the job — a
previous `exit 0` here silently reported the whole suite green.

### `scripts/run-ios-ui-tests.sh`
Swaps in the test `GoogleService-Info.plist`, builds with `UI_TEST`, forces the simulator to
English, then inside `firebase emulators:exec` runs: each `login` flow with a full
uninstall/keychain-reset/reinstall (skipping `platform: Android`), then `after-login/free`,
then `after-login/premium` (each preceded by a reset). The production plist is restored on exit.

### CI devices & failure reporting
- **Devices mirror the table in §0:** Android **API 34 / `google_apis` / x86_64** (API 35/36
  emulators proved unstable under swiftshader on the Ubuntu runners — the device went
  `offline` mid-flow; API 37 only ships as an arm64 `ps16k` image, unusable on x86 CI — so
  API 34 is the newest level with a stable, KVM-accelerated `google_apis` x86_64 image); iOS
  **iPhone 17 Pro** (falling back to the first available iPhone if that model isn't installed).
- **Failures surface in the Actions report.** Both runners emit a per-suite **JUnit XML**
  (`--format=junit --output report.xml`). The workflows publish it via `dorny/test-reporter`
  (a check listing failing flows) and `scripts/junit-summary.sh` writes a pass/fail table to
  `$GITHUB_STEP_SUMMARY`. HTML/debug output + screenshots are still uploaded as artifacts.

### Local emulator setup (reproduce CI locally)
- `scripts/setup-local-android-emulator.sh` — creates/boots an AVD matching CI (API 37,
  `google_apis`, `pixel_9`; `arm64-v8a` on Apple Silicon, `x86_64` otherwise), disables
  animations + the stylus tutorial. Then run `scripts/run-android-ui-tests.sh`.
- `scripts/setup-local-ios-simulator.sh [device]` — creates/boots the `iPhone 17 Pro`
  simulator (or the given device) on the newest installed iOS runtime, forces English. Then
  run `scripts/run-ios-ui-tests.sh "iPhone 17 Pro"`.

---

## 10. Coverage summary

| Area | Free | Premium |
|---|---|---|
| Auth (Google/Apple) | L1–L2 | L3 (iOS) — n/a |
| Onboarding / create book | L4–L6 | P3 |
| Record calculator & add-record | R1–R7 | via P2/P6/P8 |
| Book selector / limits / invite | B1–B3 | P3 |
| Categories add / duplicate / unsaved guard | C1/C2/C5/C6 | shared |
| Overview / mode / drill / export | O1–O8 | P4 |
| Records edit/delete/duplicate/sort | E1–E5 | P8 (batch) |
| Search + period gating | S1–S4 | P5 |
| Settings rows | ST1–ST15 | P6/P7/P8/P9/P10 |
| Currency (book/preferred) | ST5 | P6 |
| Color tones | ST8 | P7 |
| Ads (banner) | A1 | P1–P2 |
| Notification permission | N1 | — |
| Premium paywall entry points | B2, S4, ST12, ST13 | — |
| Premium unlocked experience | — | P1–P10 |

### Out of scope / intentionally omitted
- **C3/C4/C7** (rename/delete/reorder categories), **O4** (custom period upsell for free),
  **ST6/ST10/ST11/ST16** (preferred-currency free gate on Record, calculator-button &
  chart-mode dropdowns, delete account): covered indirectly or omitted to keep the suite
  stable; the premium counterparts (P4/P6/P8) and the paywall-gate flows exercise the same
  code paths.
- The insider **Push Notifications** admin tool (internal, not an end-user feature).
- Completing a real RevenueCat purchase (dashboard-driven paywall cannot transact in the
  emulator; entry into the paywall is asserted instead).
