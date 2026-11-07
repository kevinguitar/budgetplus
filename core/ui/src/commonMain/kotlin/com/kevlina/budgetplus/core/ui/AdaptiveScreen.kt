package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kevlina.budgetplus.core.adaptive.WindowSizeClass

@Composable
fun AdaptiveScreen(
    modifier: Modifier,
    regularContent: @Composable () -> Unit,
    wideContent: @Composable () -> Unit,
    packedContent: @Composable () -> Unit = regularContent,
    extraContent: @Composable (BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        val windowSizeClass = WindowSizeClass.calculate()
        when {
            windowSizeClass.isWide -> wideContent()

            windowSizeClass.height == WindowSizeClass.Size.Compat -> packedContent()
            else -> regularContent()
        }

        if (extraContent != null) {
            extraContent()
        }
    }
}