package com.kevlina.budgetplus.core.ads

import GoogleMobileAds.GADAdSizeBanner
import GoogleMobileAds.GADBannerView
import GoogleMobileAds.GADBannerViewDelegateProtocol
import GoogleMobileAds.GADInterstitialAd
import GoogleMobileAds.GADRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.theme.LocalAppColors
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.coroutines.flow.collect
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeBannerAd(
    adUnitId: String,
    modifier: Modifier,
    onStateUpdate: (AdBannerState) -> Unit,
) {
    val delegate = remember {
        object : NSObject(), GADBannerViewDelegateProtocol {
            override fun bannerViewDidReceiveAd(bannerView: GADBannerView) {
                onStateUpdate(AdBannerState.Loaded)
            }

            override fun bannerView(bannerView: GADBannerView, didFailToReceiveAdWithError: platform.Foundation.NSError) {
                Logger.w { "BannerAd: Failed to load. ${didFailToReceiveAdWithError.localizedDescription}" }
                onStateUpdate(AdBannerState.NotAvailable)
            }
        }
    }

    val bgColor = LocalAppColors.current.light
    UIKitView(
        factory = {
            GADBannerView(adSize = cValue { GADAdSizeBanner }).apply {
                this.adUnitID = adUnitId
                this.rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                this.delegate = delegate
                this.backgroundColor = bgColor.toUIColor()
                loadRequest(GADRequest())
            }
        },
        modifier = modifier
    )
}

private fun Color.toUIColor(): UIColor {
    return UIColor(
        red = red.toDouble(),
        green = green.toDouble(),
        blue = blue.toDouble(),
        alpha = alpha.toDouble(),
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun HandleInterstitialAd(
    adUnitId: String,
    handler: InterstitialAdsHandler,
) {
    LaunchedEffect(handler) {
        handler.showAdEvent.consumeEach {
            GADInterstitialAd.loadWithAdUnitID(
                adUnitID = adUnitId,
                request = GADRequest(),
                completionHandler = { ad, error ->
                    if (ad != null) {
                        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                        if (rootViewController != null) {
                            ad.presentFromRootViewController(rootViewController)
                        }
                    }
                    if (error != null) {
                        Logger.e { "InterstitialAd: Load failed with error: $error" }
                    }
                }
            )
        }.collect()
    }
}
