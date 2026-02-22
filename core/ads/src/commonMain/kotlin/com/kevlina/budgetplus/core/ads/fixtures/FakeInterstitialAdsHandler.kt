package com.kevlina.budgetplus.core.ads.fixtures

import androidx.annotation.VisibleForTesting
import com.kevlina.budgetplus.core.ads.InterstitialAdsHandler
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.MutableEventFlow

@VisibleForTesting
class FakeInterstitialAdsHandler : InterstitialAdsHandler {

    override val showAdEvent: EventFlow<Unit> = MutableEventFlow()

    var count = 0

    override fun showAd() {
        count++
    }
}