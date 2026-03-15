package com.kevlina.budgetplus.feature.add.record.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.common.EventTrigger
import com.kevlina.budgetplus.core.lottie.loadLottieSpec
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.theme.withTypographyScale
import com.kevlina.budgetplus.core.ui.AppTheme
import io.github.alexzhirkevich.compottie.ExperimentalCompottieApi
import io.github.alexzhirkevich.compottie.dynamic.rememberLottieDynamicProperties
import io.github.alexzhirkevich.compottie.rememberLottieAnimatable
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.seconds

@Composable
fun BoxScope.DoneAnimator(eventTrigger: EventTrigger<Unit>) {
    var showAnimation by remember { mutableStateOf(false) }
    val imgDone by rememberLottieComposition { loadLottieSpec("img_done") }
    val lottieAnimatable = rememberLottieAnimatable()

    val bgColor = LocalAppColors.current.dark
    val strokeColor = LocalAppColors.current.light

    @OptIn(ExperimentalCompottieApi::class)
    val dynamicProperties = rememberLottieDynamicProperties {
        shapeLayer("Rectangle 6 Copy") {
            // The big circle outline
            stroke("Rectangle 6 Copy", "Stroke 1") {
                color { bgColor }
            }
            // The big circle filled color
            fill("Rectangle 6 Copy", "Fill 1") {
                color { bgColor }
            }
        }

        // The check mark stroke
        shapeLayer("Path 2") {
            stroke("Path 2", "Stroke 1") {
                color { strokeColor }
            }
        }
    }

    LaunchedEffect(key1 = eventTrigger) {
        eventTrigger.event.onEach {
            showAnimation = true
            lottieAnimatable.animate(
                composition = imgDone,
                speed = 1.2F
            )
            showAnimation = false
        }.collect()
    }

    if (showAnimation) {
        Image(
            painter = rememberLottiePainter(
                composition = imgDone,
                progress = { lottieAnimatable.progress },
                dynamicProperties = dynamicProperties,
            ),
            contentDescription = null,
            modifier = Modifier
                .size(160.dp.withTypographyScale())
                .align(Alignment.Center),
        )
    }
}

@Preview
@Composable
private fun DoneAnimator_Preview() = AppTheme(themeColors = ThemeColors.Lavender) {
    val eventTrigger = remember { EventTrigger<Unit>() }
    Box {
        DoneAnimator(eventTrigger = eventTrigger)
    }
    LaunchedEffect(key1 = Unit) {
        while (true) {
            delay(1.seconds)
            eventTrigger.sendEvent(Unit)
        }
    }
}