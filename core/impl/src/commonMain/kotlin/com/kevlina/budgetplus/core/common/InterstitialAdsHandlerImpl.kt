package com.kevlina.budgetplus.core.common

import com.kevlina.budgetplus.core.ads.InterstitialAdsHandler
import com.kevlina.budgetplus.core.data.AuthManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class InterstitialAdsHandlerImpl(
    private val authManager: AuthManager,
    private val tracker: Tracker,
) : InterstitialAdsHandler {

    final override val showAdEvent: EventFlow<Unit>
        field = MutableEventFlow()

    override fun showAd() {
        if (authManager.isPremium.value) return

        showAdEvent.sendEvent()
        tracker.logEvent("show_ad_full_screen")
    }
}