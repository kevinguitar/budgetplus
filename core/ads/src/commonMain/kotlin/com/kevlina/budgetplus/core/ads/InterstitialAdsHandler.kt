package com.kevlina.budgetplus.core.ads

import com.kevlina.budgetplus.core.common.EventFlow

interface InterstitialAdsHandler {
    val showAdEvent: EventFlow<() -> Unit>

    fun showAd()

    /**
     *  Show an interstitial ad and invoke [onComplete] after the user finishes watching it.
     *  If the user is premium, [onComplete] is called immediately without showing any ad.
     *  If the ad fails to load or show, [onComplete] is still called so the guarded action proceeds.
     */
    fun showAdThen(onComplete: () -> Unit)
}