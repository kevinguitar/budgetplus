package com.kevlina.budgetplus.core.ads

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.kevlina.budgetplus.core.common.Logger
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
    val slot = remember(adUnitId) {
        PreloadedAdSlot<InterstitialAd> { onLoaded, onFailed ->
            InterstitialAd.load(
                context.applicationContext,
                adUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        onLoaded(interstitialAd)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Logger.w("InterstitialAd: Failed to load. $adError")
                        onFailed()
                    }
                }
            )
        }
    }

    LaunchedEffect(slot, handler) {
        slot.preload()
        handler.showAdEvent.consumeEach { request ->
            val activity = context as? Activity
            val interstitialAd = slot.take()
            if (interstitialAd == null || activity == null) {
                request.onNotReady()
                request.onFinished()
                slot.preload()
                return@consumeEach
            }

            interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    request.onShown()
                }

                override fun onAdDismissedFullScreenContent() {
                    request.onFinished()
                    slot.preload()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Logger.w("InterstitialAd: Failed to show. $adError")
                    request.onFinished()
                    slot.preload()
                }
            }
            interstitialAd.show(activity)
        }.collect()
    }
}
