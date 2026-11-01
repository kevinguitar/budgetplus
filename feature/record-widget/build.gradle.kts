plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.compose.multiplatform)
    alias(budgetplus.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.theme)
            implementation(projects.core.ui)
            implementation(libs.firebase.firestore)
            implementation(libs.kotlin.serialization)
        }
        androidMain.dependencies {
            implementation(libs.glance.appwidget)
            implementation(libs.glance.appwidget.preview)
            implementation(libs.glance.preview)
        }
    }
    android.androidResources.enable = true
}
