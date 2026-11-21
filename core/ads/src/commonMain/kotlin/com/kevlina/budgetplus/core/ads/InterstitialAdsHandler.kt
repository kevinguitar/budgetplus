package com.kevlina.budgetplus.core.ads

import com.kevlina.budgetplus.core.common.EventFlow
import kotlinx.coroutines.flow.StateFlow

interface InterstitialAdsHandler {

    /**
     * Whether the user can see interstitial ads at all: a free user past the new user grace period,
     * with the ads consent resolved and at least one placement enabled. Ads are only preloaded
     * when this is true.
     */
    val isEligible: StateFlow<Boolean>

    val showAdEvent: EventFlow<InterstitialAdRequest>

    /**
     * Shows a preloaded interstitial ad at the given [placement], if the placement is enabled and
     * the frequency caps allow it. Nothing happens when no ad is ready, the user never waits for one.
     */
    fun showAd(placement: InterstitialPlacement)

    /**
     * Same as [showAd], and invokes [onComplete] once the ad is closed. [onComplete] is invoked
     * right away when no ad is shown, so the guarded action always proceeds.
     */
    fun showAdThen(placement: InterstitialPlacement, onComplete: () -> Unit)
}

class InterstitialAdRequest(
    /** Called when the ad is presented on the screen. */
    val onShown: () -> Unit,
    /** Called when the ad can't be shown because it isn't loaded yet. */
    val onNotReady: () -> Unit,
    /** Always called exactly once: after the ad is closed, fails to show, or isn't ready. */
    val onFinished: () -> Unit,
)
