package com.kevlina.budgetplus.insider.app.main

import android.content.Context
import com.kevlina.budgetplus.core.common.nav.InsiderDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.insiderApp.BuildConfig
import com.kevlina.budgetplus.insiderApp.R
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
object InsiderProviders {

    @SingleIn(AppScope::class)
    @Provides
    fun provideNavController(): NavController<InsiderDest> {
        return NavController(startRoot = InsiderDest.Insider)
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

    @Provides
    @Named("google_client_id")
    fun provideGoogleClientId(context: Context): String {
        return context.getString(R.string.google_cloud_client_id)
    }
}