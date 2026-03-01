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
        }
        androidMain.dependencies {
            implementation("com.google.cloud:google-cloud-translate:2.81.0") {
                // Exclude artifacts that caused duplicated classes :/
                exclude(group = "com.google.api.grpc", module = "proto-google-common-protos")
                exclude(group = "com.google.protobuf", module = "protobuf-java")
            }
        }
    }
}