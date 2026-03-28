package com.kevlina.budgetplus.insider.app.main

import com.kevlina.budgetplus.insiderApp.BuildConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface InsiderModule {

    // Do not override FCM token from insider app, to make sure regular users
    // can still receive push notifications from the androidMain B+ app.
    @Provides
    @Named("allow_update_fcm_token")
    fun provideAllowUpdateFcmToken(): Boolean = false

    @Provides
    @Named("google_api_key")
    fun provideGoogleApiKey(): String = BuildConfig.GOOGLE_API_KEY

    @Provides
    @Named("is_debug")
    fun provideIsDebug(): Boolean = BuildConfig.DEBUG
}