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
            implementation(projects.core.data)
            implementation(projects.core.ui)
            implementation(libs.compottie)
        }

        androidMain.dependencies {
            implementation(libs.google.ads)
        }
    }
}

swiftPackageConfig {
    create("nativeBridge") {
        dependency {
            minIos = "16.6"
            linkerOpts = listOf("-ObjC")
            remotePackageVersion(
                url = uri("https://github.com/googleads/swift-package-manager-google-mobile-ads.git"),
                products = {
                    add("GoogleMobileAds", exportToKotlin = true)
                },
                version = libs.versions.google.ads.ios.get()
            )
        }
    }
}
