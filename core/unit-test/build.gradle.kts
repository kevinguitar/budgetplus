plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(kotlin("test"))
            implementation(libs.coroutines.test)
        }
        androidMain.dependencies {
            api(kotlin("test-junit"))
            implementation(libs.junit.compose)
        }
    }
}