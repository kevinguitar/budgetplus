package com.kevlina.budgetplus.core.ads

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import co.touchlab.kermit.Logger
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.kevlina.budgetplus.core.common.consumeEach
import kotlinx.coroutines.flow.collect

@Composable
actual fun NativeBannerAd(
    adUnitId: String,
    modifier: Modifier,
    onStateUpdate: (AdBannerState) -> Unit,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        onStateUpdate(AdBannerState.NotAvailable)
                    }

                    override fun onAdLoaded() {
                        onStateUpdate(AdBannerState.Loaded)
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
actual fun HandleInterstitialAd(
    adUnitId: String,
    handler: InterstitialAdsHandler,
) {
    val context = LocalContext.current
    LaunchedEffect(handler) {
        handler.showAdEvent.consumeEach { onComplete ->
            InterstitialAd.load(
                context,
                adUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                onComplete()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                onComplete()
                            }
                        }
                        interstitialAd.show(context as Activity)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Logger.e { "InterstitialAd failed to load: $adError" }
                        onComplete()
                    }
                }
            )
        }.collect()
    }
}
