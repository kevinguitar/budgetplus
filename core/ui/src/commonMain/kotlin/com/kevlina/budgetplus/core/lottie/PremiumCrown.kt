package com.kevlina.budgetplus.core.lottie

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.kevlina.budgetplus.core.theme.typographyScale
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

@Composable
fun PremiumCrown(modifier: Modifier = Modifier) {
    val icPremium by rememberLottieComposition {
        loadLottieSpec("ic_premium")
    }
    Image(
        painter = rememberLottiePainter(
            composition = icPremium,
            iterations = Compottie.IterateForever,
        ),
        contentDescription = null,
        modifier = modifier.typographyScale()
    )
}