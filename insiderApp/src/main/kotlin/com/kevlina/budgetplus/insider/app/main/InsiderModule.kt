package com.kevlina.budgetplus.insider.app.main

import android.content.Context
import android.content.Intent
import com.kevlina.budgetplus.core.common.nav.AndroidNavigationAction
import com.kevlina.budgetplus.core.common.nav.NavigationAction
import com.kevlina.budgetplus.insiderApp.BuildConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface InsiderModule {

    @Provides @Named("book")
    fun provideBookNavigationAction(context: Context): NavigationAction {
        return AndroidNavigationAction(intent = Intent(context, InsiderActivity::class.java))
    }

    @Provides @Named("welcome")
    fun provideWelcomeNavigationAction(context: Context): NavigationAction {
        return AndroidNavigationAction(intent = Intent(context, InsiderActivity::class.java))
    }

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