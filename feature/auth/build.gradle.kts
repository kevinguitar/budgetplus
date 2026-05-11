plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.compose.multiplatform)
    alias(budgetplus.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.ui)

            implementation(libs.firebase.auth)
        }
        androidMain.dependencies {
            implementation(libs.android.credentials)
            // For devices running Android 13 and below
            // See: https://developer.android.com/jetpack/androidx/releases/credentials
            implementation(libs.android.credentials.play.services)
            implementation(libs.install.referrer)
            implementation(libs.google.id)
        }
    }
    android.androidResources.enable = true
}