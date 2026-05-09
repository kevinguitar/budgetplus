plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.compose.multiplatform)
    alias(budgetplus.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.ui)
            implementation(projects.core.data)
            implementation(projects.core.settingsApi)

            implementation(libs.compottie)
            implementation(libs.permissions.compose)
            implementation(libs.permissions.microphone)
        }
    }
}