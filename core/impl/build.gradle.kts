plugins {
    alias(budgetplus.plugins.kotlin.multiplatform)
    alias(budgetplus.plugins.metro)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.inappUpdate)
            implementation(projects.core.inappReview)
            implementation(projects.core.ui)

            implementation(libs.crashkios)
            implementation(libs.kermit.crashlytics)
            implementation(libs.kotlin.serialization)
            implementation(libs.ktor.core)
            implementation(libs.compose.resources)

            implementation(libs.firebase.auth)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.functions)
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.config)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.google.play.update)
            implementation(libs.google.play.review)
            implementation(libs.ktor.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.ios)
        }
    }
}