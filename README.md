# Budget+ Multiplatform App（極簡記帳）

[![CI](https://github.com/kevinguitar/budgetplus/actions/workflows/ci.yml/badge.svg)](https://github.com/kevinguitar/budgetplus/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-PolyForm%20Noncommercial%201.0.0-blue.svg)](https://polyformproject.org/licenses/noncommercial/1.0.0)

Budget+ is an easy-to-use co-spending tracker to track expenses together with your friends and family.

### Now available on both Android and iOS!

[![Android](https://img.shields.io/badge/Android-Google%20Play-green.svg?logo=android)](https://play.google.com/store/apps/details?id=com.kevlina.budgetplus)
[![iOS](https://img.shields.io/badge/iOS-App%20Store-black.svg?logo=apple)](https://apps.apple.com/app/id6759791430)

## Feature Overview

![overview_1](https://github.com/user-attachments/assets/29101061-de8c-4007-954a-f9eb03419d53)

![overview_2](https://github.com/user-attachments/assets/782a20cc-8460-416a-b0be-7a9afdbdb5d7)

### Some cool animated features!! ✨
|                                                      Pie Chart                                                       |                                                  Color Tone Picker                                                   |                                                 Customize Color Tone                                                 |
|:--------------------------------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------:| 
| <video src="https://github.com/kevinguitar/budgetplus-android/assets/18852983/c2222bc4-f78e-42a2-a78b-ebdb78cc7c2e"> | <video src="https://github.com/kevinguitar/budgetplus-android/assets/18852983/82f6d3e9-8162-4554-809e-e2f81913e684"> | <video src="https://github.com/kevinguitar/budgetplus-android/assets/18852983/bece0f6d-2f49-4562-b24e-4ff51cf5d5a8"> |

---

## Tech Stack

### Business Logic and Core
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) for shared logic across Android and iOS
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) for shared UI + MVVM Architecture
- [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html) for asynchronous operations
- [Metro](https://zacsweers.github.io/metro/latest/) for dependency injection
- [Navigation3](https://developer.android.com/guide/navigation/navigation-3) for Composable navigation
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for persistent data storage
- [RevenueCat](https://github.com/RevenueCat/purchases-kmp) for in-app purchases and subscriptions
- [AdMob](https://admob.google.com/home/) with Meta Audience Network mediation for ads
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) for JSON parsing
- [Kotlinx Datetime](https://github.com/Kotlin/kotlinx-datetime) for date and time handling
- [Firebase](https://firebase.google.com/)
  - [Firestore](https://firebase.google.com/docs/firestore) for real-time database
  - [Authentication](https://firebase.google.com/docs/auth) for Google and Apple ID sign-in
  - [Crashlytics](https://firebase.google.com/docs/crashlytics) for crash reporting
  - [Analytics](https://firebase.google.com/docs/analytics), [Messaging](https://firebase.google.com/docs/cloud-messaging), and [Remote Config](https://firebase.google.com/docs/remote-config)

### Open-Source Libraries for KMP
- [Calf](https://github.com/MohamedRejeb/Calf) for native iOS look and feel
- [Coil3](https://github.com/coil-kt/coil) for image loading with KMP support
- [Colorpicker-compose](https://github.com/skydoves/colorpicker-compose) for color picking
- [Compottie](https://github.com/alexzhirkevich/compottie) for Lottie animations in Compose Multiplatform
- [CrashKiOS](https://github.com/touchlab/CrashKiOS) for better crash reporting on iOS
- [Csv](https://github.com/sergejsha/csv) for exporting data as CSV (KMP)
- [Firebase Kotlin SDK](https://github.com/GitLiveApp/firebase-kotlin-sdk) for KMP support
- [Kermit](https://github.com/touchlab/Kermit) for logging
- [Keval](https://github.com/notKamui/Keval) for mathematical expression evaluation
- [Moko-permissions](https://github.com/icerockdev/moko-permissions) for KMP permission handling
- [Reorderable](https://github.com/Calvin-LL/Reorderable) for reordering items in Compose

### Android Specific
- [In-App Review](https://developer.android.com/guide/playcore/in-app-review) and [In-App Update](https://developer.android.com/guide/playcore/in-app-updates) integration
- [Baseline Profile](https://developer.android.com/topic/performance/baselineprofiles/overview) for improving app startup time

---

## Supported Deeplinks
- Home screen: https://budgetplus.cchi.tw/record
- Overview: https://budgetplus.cchi.tw/overview
- Unlock premium: https://budgetplus.cchi.tw/unlockPremium
- Settings: https://budgetplus.cchi.tw/settings
- Settings with members dialog: https://budgetplus.cchi.tw/settings?showMembers=true
- Sharing customized color tones: https://budgetplus.cchi.tw/colors?hex=cff1ff%3bdaf2cb%3b84c18f%3b596980

---

## Performance Optimization (Baseline Profile)

The project uses [Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview) to improve startup performance.

### Generating a Profile

The profile is generated automatically on every Play Store publish (see
[`.github/workflows/publish-android-app.yml`](.github/workflows/publish-android-app.yml)). It is produced by the
[`androidx.baselineprofile`](https://developer.android.com/topic/performance/baselineprofiles/configure-baselineprofiles)
plugin, which runs the `:benchmark` generator against the app's `nonMinifiedRelease`
variant. That variant sets `is_ui_test=true`, so it routes Firebase to the local
emulators and signs in anonymously (Google Sign-In is unavailable on CI).

To regenerate the profile locally:

1. Start an Android emulator and the Firebase Auth + Firestore emulators:
   ```bash
   firebase --config ui-tests/config/firebase.json --project budgetplus-ui-tests \
     emulators:start --only auth,firestore
   ```
2. Forward the emulator ports so the app can reach the Firebase emulators:
   ```bash
   adb reverse tcp:9099 tcp:9099 && adb reverse tcp:8080 tcp:8080
   ```
3. Run the generator:
   ```bash
   ./gradlew :androidApp:generateBaselineProfile
   ```

The plugin merges the result into `androidApp/src/main/generated/baselineProfiles/baseline-prof.txt`,
which is consumed by every release build. This path is git-ignored because it is a
generated artifact.

### Running Macrobenchmarks
```bash
./gradlew :benchmark:connectedReleaseAndroidTest -P android.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
```

---

## Build the Project Locally
If you want to build the project locally, follow these steps:

1. Clone the repository.
2. Add a `google-services.json` file under `:androidApp` folder by copying the dummy config already committed for the UI tests:
```bash
cp ui-tests/config/google-services.json androidApp/google-services.json
```
3. Now you should be able to build both Android and iOS apps:
   - Build the Android app, run `./gradlew :androidApp:assembleDebug`
   - Build the iOS app, run `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`
4. If you want to have the app running with real functionalities, you'll need to create a new project on Firebase and wire your API keys in the project.

---

## UI Tests

UI tests use [Maestro](https://maestro.mobile.dev/) to launch the real Android or iOS application. Both platforms run the same flows from `ui-tests/` against local Firebase Auth and Firestore emulators, so production data is never used.

The flows are split into three independent suites — `login/` (auth + onboarding), `after-login/free/`, and `after-login/premium/` — with shared subflows in `common/`. See [`ui-tests/TEST_COVERAGE.md`](ui-tests/TEST_COVERAGE.md) for the full coverage map, the test-mode hooks, and the cross-platform selector strategy.

### Adding a Test

1. Add a Maestro YAML flow under the relevant suite in `ui-tests/`. The runner scripts execute every flow in each suite.
2. Prefer stable visible text for selectors because Maestro reads it consistently from both Android and iOS accessibility trees. Add custom semantics only when no stable visible text exists, and verify the selector on both platforms.
3. Keep platform-specific commands out of flows where possible so the same test runs on Android and iOS. Gate a genuinely platform-specific flow with `platform: Android`/`platform: iOS`.

### Running Locally

Install the [Firebase CLI](https://firebase.google.com/docs/cli) and [Maestro CLI](https://docs.maestro.dev/maestro-cli/how-to-install-maestro-cli), then start an Android emulator or iOS Simulator.

Run the Android tests:

```bash
./ui-tests/scripts/run-android-ui-tests.sh
```

Run the iOS tests using an installed `iPhone 17 Pro` simulator:

```bash
./ui-tests/scripts/run-ios-ui-tests.sh "iPhone 17 Pro"
```

Pass a different simulator name as the first argument if needed (it defaults to `iPhone 17`). Both scripts temporarily install the test Firebase configuration and restore the production `google-services.json` or `GoogleService-Info.plist` from `misc/release` when they exit, including after a test failure.

---

## Backend: Firebase Cloud Functions

Database interactions and push notifications are implemented using [Firebase Cloud Functions](https://firebase.google.com/docs/functions). 

The backend repository is also open-sourced: [budgetplus-cloud-functions](https://github.com/kevinguitar/budgetplus-cloud-functions)

---

License
-------

    Copyright (c) 2024 Kevin Chiu

    Licensed under the PolyForm Noncommercial License 1.0.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    https://polyformproject.org/licenses/noncommercial/1.0.0

    This software may be used for any noncommercial purpose only.
    Commercial use, including monetization, is not permitted.
    See the License for the specific terms governing permissions and
    limitations under the License.
