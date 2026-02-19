package com.kevlina.budgetplus.core.ads

import com.kevlina.budgetplus.core.common.EventFlow

interface InterstitialAdsHandler {
    val showAdEvent: EventFlow<Unit>

    fun showAd()
}