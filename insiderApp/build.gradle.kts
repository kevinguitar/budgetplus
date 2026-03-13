plugins {
    alias(budgetplus.plugins.insider.app)
    alias(budgetplus.plugins.metro)
    alias(budgetplus.plugins.compose)
}

dependencies {
    implementation(enforcedPlatform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.firestore)

    implementation(libs.android.activity)
    implementation(libs.navigation3.ui)
    implementation(libs.navigation3.viewmodel)

    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(projects.core.impl) {
        exclude("com.google.android.gms", "play-services-ads")
    }

    implementation(projects.feature.auth)
    implementation(projects.feature.pushNotifications)
    implementation(projects.feature.insider)

    // Without this it crashes at grpc usages internally :/
    //noinspection UseTomlInstead
    implementation(enforcedPlatform("io.grpc:grpc-bom:1.79.0"))
}