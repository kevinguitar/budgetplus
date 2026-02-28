plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.compose.multiplatform)
    alias(budgetplus.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.billing)
            implementation(projects.core.data)
            implementation(projects.core.ui)

            implementation(libs.compottie)
            implementation(libs.revenuecat.ui)
        }
    }
}