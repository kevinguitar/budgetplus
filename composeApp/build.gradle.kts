plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.compose.multiplatform)
    alias(budgetplus.plugins.metro)
    alias(libs.plugins.spm.kmp)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.compilations {
            val main by getting {
                cinterops.create("nativeBridge")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.admob.kmp)
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

swiftPackageConfig {
    create("nativeBridge") {
        dependency {
            minIos = "16.6"
            linkerOpts = listOf("-ObjC")
            exportedPackageSettings { includeProduct = listOf("FirebaseFirestore") }
            remotePackageVersion(
                // Repository URL
                url = uri("https://github.com/firebase/firebase-ios-sdk.git"),
                // Libraries from the package
                products = {
                    // Export to Kotlin for use in shared Kotlin code and use it in your swift code
                    // the export doesn't work when gitlive is implemented, my guess is a bug with cinterop
                    // because gitlive already use cinterop
                    listOf(
                        "FirebaseAnalytics",
                        "FirebaseAuth",
                        "FirebaseCore",
                        "FirebaseCrashlytics",
                        "FirebaseFirestore",
                        "FirebaseMessaging",
                        "FirebaseRemoteConfig",
                    ).forEach { add(it, exportToKotlin = false) }
                },
                // Package version
                version = libs.versions.firebase.ios.get(),
            )
            remotePackageVersion(
                url = uri("https://github.com/google/GoogleSignIn-iOS.git"),
                products = {
                    add("GoogleSignIn", exportToKotlin = true)
                },
                version = libs.versions.google.signin.ios.get()
            )
        }
    }
}
