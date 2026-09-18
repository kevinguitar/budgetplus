package com.kevlina.budgetplus.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator

@Composable
actual fun InfiniteCircularProgress(
    modifier: Modifier,
    color: Color,
    strokeWidth: Dp,
) {
    AdaptiveCircularProgressIndicator(
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth,
    )
}
