package com.kevlina.budgetplus.app

import android.content.Context
import com.kevlina.budgetplus.androidApp.R
import com.kevlina.budgetplus.core.ads.AdUnitId
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface AppModule {

    @Provides
    @Named("is_debug")
    fun provideIsDebug(context: Context): Boolean {
        return context.resources.getBoolean(R.bool.is_debug)
    }

    @Provides
    fun provideAdUnitId(context: Context): AdUnitId {
        return AdUnitId(
            banner = context.getString(R.string.admob_banner_id_30sec),
            interstitial = context.getString(R.string.admob_interstitial_id)
        )
    }

    @Provides
    @Named("store_review_url")
    fun provideGooglePlayUrl(
        @Named("app_package") appPackage: String,
    ): String {
        return "https://play.google.com/store/apps/details?id=$appPackage"
    }
}