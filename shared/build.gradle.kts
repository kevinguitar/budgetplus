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

            // Calf requires its artifact to be exported to the iOS framework binary.
            export(libs.calf.ui)

            if (buildType == NativeBuildType.RELEASE) {
                binaryOption("sourceInfoType", "noop")
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
                "FirebaseFunctions",
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
            api(libs.calf.ui)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.config)
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.functions)
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
