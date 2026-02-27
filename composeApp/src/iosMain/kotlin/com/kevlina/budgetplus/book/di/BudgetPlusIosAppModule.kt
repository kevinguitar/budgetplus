package com.kevlina.budgetplus.book.di

import com.kevlina.budgetplus.core.ads.AdUnitId
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.nav.DeeplinkFlow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface BudgetPlusIosAppModule {

    @Provides
    fun provideAdUnitId(): AdUnitId = AdUnitId(
        banner = "ca-app-pub-5636675608309788/1982646025",
        interstitial = "ca-app-pub-5636675608309788/3378219950"
    )

    @Provides
    @Named("is_debug")
    fun provideIsDebug(): Boolean = true

    @Provides
    @Named("allow_update_fcm_token")
    fun provideAllowUpdateFcmToken(): Boolean = true

    @Provides
    @SingleIn(AppScope::class)
    fun provideDeeplinkFlow(): DeeplinkFlow = MutableEventFlow()
}