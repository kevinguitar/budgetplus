import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.compose.multiplatform)
    alias(budgetplus.plugins.metro)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true

            binaryOption("bundleId", "Shared")

            // calf-ui is deliberately not export()ed, although Calf's docs say iOS requires it.
            // An export gives every public declaration an Objective-C bridge that survives dead
            // code elimination. For Calf that kept its whole SFSymbol catalog and every Skiko
            // and Compose type its API mentions alive: ~6.7 MB of release binary.
            //
            // The export only matters for Calf components whose Kotlin objects receive UIKit
            // callbacks (@ObjCAction targets or delegate protocols). Without it they crash in
            // Release with "unrecognized selector sent to instance" (Calf #189, #330). In Calf
            // 0.14.0 that is every component in the datepicker, dialog, fab, navigation, sheet
            // and timepicker packages. The ones we use (switches, buttons, dropdown, progress
            // indicator, adaptiveClickable, UIKitImage) don't need it.
            //
            // iosApp/scripts/test_calf_export.py fails CI when code uses one of those packages
            // while this export is off, and when Calf is upgraded without a re-audit. To adopt
            // one of them, add `export(libs.calf.ui)` back here and accept the size cost.

            if (buildType == NativeBuildType.RELEASE) {
                binaryOption("sourceInfoType", "noop")
                // Optimize release code for size (-Oz) and store ASCII string literals as
                // Latin-1. Together they cut ~7.9 MB from the release binary. Both are
                // Experimental: if a regression shows up, drop them here first.
                // https://kotlinlang.org/docs/native-binary-options.html
                binaryOption("smallBinary", "true")
                binaryOption("latin1Strings", "true")
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    swiftPMDependencies {
        iosMinimumDeploymentTarget.set("16.6")
        // GitLive already provides Firebase cinterops. Only import our own native APIs.
        discoverClangModulesImplicitly.set(false)
        swiftPackage(
            url = url("https://github.com/firebase/firebase-ios-sdk.git"),
            version = exact("12.19.3"),
            products = listOf(
                "FirebaseAnalytics",
                "FirebaseAuth",
                "FirebaseCore",
                "FirebaseCrashlytics",
                "FirebaseFirestore",
                "FirebaseMessaging",
                "FirebaseRemoteConfig",
            ).map { product(it) },
            importedClangModules = emptyList(),
        )
        swiftPackage(
            url = url("https://github.com/google/GoogleSignIn-iOS.git"),
            version = exact("10.1.0"),
            products = listOf(product("GoogleSignIn")),
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compottie)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.config)
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.messaging)
            implementation(libs.navigation3.ui)
            implementation(libs.navigation3.viewmodel)

            api(projects.core.ads)
            api(projects.core.billing)
            api(projects.core.data)
            api(projects.core.impl)
            api(projects.core.inappUpdate)
            api(projects.core.notification)
            api(projects.core.settingsApi)
            api(projects.core.ui)

            api(projects.feature.addRecord)
            api(projects.feature.auth)
            api(projects.feature.categoryPills)
            api(projects.feature.colorTonePicker)
            api(projects.feature.currencyPicker)
            api(projects.feature.editCategory)
            api(projects.feature.overview)
            api(projects.feature.recordCard)
            api(projects.feature.recordWidget)
            api(projects.feature.records)
            api(projects.feature.search)
            api(projects.feature.settings)
            api(projects.feature.speakToRecord)
            api(projects.feature.unlockPremium)
            api(projects.feature.welcome)
        }
        androidMain.dependencies {
            implementation(libs.google.ads)
            runtimeOnly(libs.google.ads.mediation.meta)
        }
    }
}
