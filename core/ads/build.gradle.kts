plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.metro)
}

//TODO: Remove this module
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
        }
    }
}