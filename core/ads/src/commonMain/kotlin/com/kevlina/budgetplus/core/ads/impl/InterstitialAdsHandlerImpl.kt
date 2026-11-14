package com.kevlina.budgetplus.core.ads.impl

import com.kevlina.budgetplus.core.ads.AdmobInitializer
import com.kevlina.budgetplus.core.ads.AdsAvailability
import com.kevlina.budgetplus.core.ads.InterstitialAdsHandler
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.data.AuthManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class InterstitialAdsHandlerImpl(
    private val authManager: AuthManager,
    private val admobInitializer: AdmobInitializer,
    private val tracker: Tracker,
) : InterstitialAdsHandler {

    override val showAdEvent: EventFlow<() -> Unit>
        field = MutableEventFlow()

    // Ads can't be requested before the user's consent is resolved.
    private val canShowAd: Boolean
        get() = !authManager.isPremium.value &&
            admobInitializer.adsAvailability.value == AdsAvailability.Available

    override fun showAd() {
        if (!canShowAd) return

        showAdEvent.sendEvent {}
        tracker.logEvent("show_ad_full_screen")
    }

    override fun showAdThen(onComplete: () -> Unit) {
        if (!canShowAd) {
            onComplete()
            return
        }

        showAdEvent.sendEvent(onComplete)
        tracker.logEvent("show_ad_full_screen")
    }
}