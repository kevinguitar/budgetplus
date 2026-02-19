plugins {
    alias(budgetplus.plugins.android.app)
    alias(budgetplus.plugins.metro)
    alias(budgetplus.plugins.compose)
}

dependencies {
    implementation(enforcedPlatform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.coil.okhttp)
    implementation(libs.profile.installer)

    implementation(projects.composeApp)
}