package com.kevlina.budgetplus.core.ads.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.ads.InterstitialAdRequest
import com.kevlina.budgetplus.core.ads.InterstitialAdsHandler
import com.kevlina.budgetplus.core.ads.InterstitialPlacement
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.MutableEventFlow
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeInterstitialAdsHandler(
    isEligible: Boolean = true,
) : InterstitialAdsHandler {

    override val isEligible = MutableStateFlow(isEligible)

    override val showAdEvent: EventFlow<InterstitialAdRequest> = MutableEventFlow()

    val placements = mutableListOf<InterstitialPlacement>()

    val count get() = placements.size

    override fun showAd(placement: InterstitialPlacement) {
        placements += placement
    }

    override fun showAdThen(placement: InterstitialPlacement, onComplete: () -> Unit) {
        placements += placement
        onComplete()
    }
}
