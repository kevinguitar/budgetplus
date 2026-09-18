package com.kevlina.budgetplus.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors

/**
 * Adaptive infinite circular progress indicator.
 *
 * On Android it renders the Material [androidx.compose.material3.CircularProgressIndicator]
 * (unchanged), while on iOS it renders Calf's `AdaptiveCircularProgressIndicator`
 * which mimics the native `UIActivityIndicatorView`.
 */
@Composable
expect fun InfiniteCircularProgress(
    modifier: Modifier = Modifier,
    color: Color = LocalAppColors.current.dark,
    strokeWidth: Dp = 4.dp,
)
