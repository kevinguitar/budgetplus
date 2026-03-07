# Budget+ Multiplatform App（極簡記帳）

[![Android CI](https://github.com/kevinguitar/budgetplus/actions/workflows/android-ci.yml/badge.svg)](https://github.com/kevinguitar/budgetplus/actions/workflows/android-ci.yml)
[![iOS CI](https://github.com/kevinguitar/budgetplus/actions/workflows/ios-ci.yml/badge.svg)](https://github.com/kevinguitar/budgetplus/actions/workflows/ios-ci.yml)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Budget+ is an easy-to-use co-spending tracker to track expenses together with your friends and family.

### Now available on both Android and iOS!

[![Android](https://img.shields.io/badge/Android-Google%20Play-green.svg?logo=android)](https://play.google.com/store/apps/details?id=com.kevlina.budgetplus)
[![iOS](https://img.shields.io/badge/iOS-App%20Store-black.svg?logo=apple)](https://apps.apple.com/app/id6759791430)

## Feature Overview

![overview_1](https://github.com/user-attachments/assets/66d5835d-f81f-456a-920c-43c6542977ee)

![overview_2](https://github.com/user-attachments/assets/a480d869-a814-41f1-a258-e24e61e38f2e)

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
- [Coil3](https://github.com/coil-kt/coil) for image loading with KMP support
- [Colorpicker-compose](https://github.com/skydoves/colorpicker-compose) for color picking
- [Compottie](https://github.com/alexzhirkevich/compottie) for Lottie animations in Compose Multiplatform
- [CrashKiOS](https://github.com/touchlab/CrashKiOS) for better crash reporting on iOS
- [Csv](https://github.com/sergejsha/csv) for exporting data as CSV (KMP)
- [Detekt](https://github.com/detekt/detekt) for Kotlin static code analysis
- [Firebase Kotlin SDK](https://github.com/GitLiveApp/firebase-kotlin-sdk) for KMP support
- [Kermit](https://github.com/touchlab/Kermit) for logging
- [Keval](https://github.com/notKamui/Keval) for mathematical expression evaluation
- [MockK](https://github.com/mockk/mockk) for mocking classes in unit tests
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
To run the generator:
```bash
./gradlew :benchmark:connectedReleaseAndroidTest -P android.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
```

The output can be found in: `[module]/build/outputs/connected_android_test_additional_output/release/connected/[device]`.

Copy and rename the file to `baseline-prof.txt` and place it in the `src/main` directory of your app module.

### Running Macrobenchmarks
```bash
./gradlew :benchmark:connectedReleaseAndroidTest -P android.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
```

---

## Backend: Firebase Cloud Functions

Database interactions and push notifications are implemented using [Firebase Cloud Functions](https://firebase.google.com/docs/functions). 

The backend repository is also open-sourced: [budgetplus-cloud-functions](https://github.com/kevinguitar/budgetplus-cloud-functions)

---

## License

This project is licensed under the MIT License.

```
Copyright (c) 2024 kevinguitar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
