import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.compose.multiplatform)
    alias(budgetplus.plugins.metro)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    swiftPMDependencies {
        iosMinimumDeploymentTarget.set("16.6")
        discoverClangModulesImplicitly.set(false)
        swiftPackage(
            url = url("https://github.com/googleads/swift-package-manager-google-mobile-ads.git"),
            version = exact("13.12.0"),
            products = listOf(product("GoogleMobileAds")),
        )
        // User Messaging Platform, for the GDPR consent. GoogleMobileAds already depends on it,
        // declare it explicitly to import its clang module.
        swiftPackage(
            url = url("https://github.com/googleads/swift-package-manager-google-user-messaging-platform.git"),
            version = exact("3.1.0"),
            products = listOf(product("GoogleUserMessagingPlatform")),
            importedClangModules = listOf("UserMessagingPlatform"),
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.ui)
            implementation(libs.compottie)
        }

        androidMain.dependencies {
            implementation(libs.google.ads)
            implementation(libs.google.ads.ump)
        }
    }
}
