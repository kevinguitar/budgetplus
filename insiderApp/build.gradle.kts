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
    implementation(libs.coil.okhttp)

    implementation(projects.core.data)
    implementation(projects.core.impl)
    implementation(projects.core.ui)

    implementation(projects.feature.auth)
    implementation(projects.feature.pushNotifications)
    implementation(projects.feature.insider)
}