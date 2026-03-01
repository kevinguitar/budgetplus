package com.kevlina.budgetplus.core.di

import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.nav.APP_DEEPLINK
import com.kevlina.budgetplus.core.common.nav.NAV_RECORD_PATH
import com.kevlina.budgetplus.core.common.nav.NavigationFlow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface CommonModule {

    // Make it a singleton here for easiness, there is no activity stack at all.
    @Provides
    @SingleIn(AppScope::class)
    fun provideNavigationFlow(): NavigationFlow = MutableEventFlow()

    @Provides
    @Named("app_package")
    fun provideAppPackage(): String = "com.kevlina.budgetplus"

    @Provides
    @Named("share_app_url")
    fun provideGooglePlayUrl(): String = "$APP_DEEPLINK/get-app"

    @Provides
    @Named("instagram_url")
    fun provideInstagramUrl(): String = "https://www.instagram.com/budget.plus.tw/"

    @Provides
    @Named("contact_email")
    fun provideContactEmail(): String = "budgetplussg@gmail.com"

    @Provides
    @Named("privacy_policy_url")
    fun providePrivacyPolicyUrl(): String = "https://budgetplus.cchi.tw/privacy-policy/"

    @Provides
    @Named("default_deeplink")
    fun provideDefaultDeeplink(): String = "$APP_DEEPLINK/$NAV_RECORD_PATH"
}