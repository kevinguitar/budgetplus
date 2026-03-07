package com.kevlina.budgetplus.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun NativeBannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
    onStateUpdate: (AdBannerState) -> Unit
)

@Composable
expect fun HandleInterstitialAd(
    adUnitId: String,
    handler: InterstitialAdsHandler,
)
