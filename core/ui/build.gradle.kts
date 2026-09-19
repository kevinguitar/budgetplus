plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.compose.multiplatform)
    alias(budgetplus.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.theme)
            implementation(libs.calf.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.material3.windowsizeclass)

            implementation(libs.compottie)
            implementation(libs.compottie.dot)
            implementation(libs.compottie.resources)
        }
    }
    compilerOptions {
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi")
    }
    android.androidResources.enable = true
}