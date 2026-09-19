package com.kevlina.budgetplus.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator

@Composable
fun InfiniteCircularProgress(
    modifier: Modifier = Modifier,
    color: Color = LocalAppColors.current.dark,
    strokeWidth: Dp = 4.dp,
) {
    AdaptiveCircularProgressIndicator(
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth,
    )
}
