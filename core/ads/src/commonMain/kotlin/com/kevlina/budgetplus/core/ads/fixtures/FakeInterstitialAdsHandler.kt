package com.kevlina.budgetplus.core.ads.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.ads.InterstitialAdsHandler
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.MutableEventFlow

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeInterstitialAdsHandler : InterstitialAdsHandler {

    override val showAdEvent: EventFlow<() -> Unit> = MutableEventFlow()

    var count = 0

    override fun showAd() {
        count++
    }

    override fun showAdThen(onComplete: () -> Unit) {
        count++
        onComplete()
    }
}