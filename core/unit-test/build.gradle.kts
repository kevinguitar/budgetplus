plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
        androidMain.dependencies {
            implementation(kotlin("test-junit"))
        }
    }
}