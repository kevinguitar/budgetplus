package com.kevlina.budgetplus.book.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.lexilabs.basic.ads.AdSize
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.BannerAd
import app.lexilabs.basic.ads.composable.rememberBannerAd
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ads_not_available
import com.kevlina.budgetplus.core.lottie.loadLottieSpec
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.Text
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.ExperimentalCompottieApi
import io.github.alexzhirkevich.compottie.dynamic.rememberLottieDynamicProperties
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.stringResource

@OptIn(DependsOnGoogleMobileAds::class)
@Composable
internal fun AdsBanner(bannerId: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(50.dp)
            .fillMaxWidth()
    ) {
        var adBannerState by remember { mutableStateOf(AdBannerState.Loading) }
        val adHandler by rememberBannerAd(
            adUnitId = bannerId,
            adSize = AdSize.BANNER,
            onLoad = { adBannerState = AdBannerState.Loaded },
            onFailure = { adBannerState = AdBannerState.NotAvailable },
        )

        when (adBannerState) {
            AdBannerState.Loading -> AdsBannerLoader()
            AdBannerState.NotAvailable -> AdsBannerNotAvailable()
            AdBannerState.Loaded -> BannerAd(adHandler)
        }
    }
}

private const val LOADER_SCALE = 2.5F

@Composable
private fun AdsBannerLoader() {
    val composition by rememberLottieComposition { loadLottieSpec("img_loader") }
    val dotColor = LocalAppColors.current.dark

    @OptIn(ExperimentalCompottieApi::class)
    val dynamicProperties = rememberLottieDynamicProperties {
        shapeLayer("**") {
            fill("Ellipse 1", "Fill 1") {
                color { dotColor }
            }
        }
    }

    Image(
        painter = rememberLottiePainter(
            composition = composition,
            iterations = Compottie.IterateForever,
            dynamicProperties = dynamicProperties,
        ),
        contentDescription = null,
        modifier = Modifier
            .fillMaxHeight()
            .scale(LOADER_SCALE)
    )
}

@Composable
private fun AdsBannerNotAvailable() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            imageVector = Icons.Rounded.SearchOff,
            tint = LocalAppColors.current.dark,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = stringResource(Res.string.ads_not_available),
            color = LocalAppColors.current.dark,
        )
    }
}

internal enum class AdBannerState {
    Loading, NotAvailable, Loaded
}

@Preview(heightDp = 50, showBackground = true)
@Composable
private fun AdsBannerLoader_Preview() = AppTheme(themeColors = ThemeColors.NemoSea) {
    AdsBannerLoader()
}

@Preview(showBackground = true)
@Composable
private fun AdsBannerNotAvailable_Preview() = AppTheme {
    AdsBannerNotAvailable()
}