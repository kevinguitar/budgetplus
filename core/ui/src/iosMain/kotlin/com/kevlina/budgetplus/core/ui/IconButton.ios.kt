package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.kevlina.budgetplus.core.theme.withTypographyScale
import com.mohamedrejeb.calf.ui.button.AdaptiveIconButton

@Composable
actual fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier,
    size: Dp,
    enabled: Boolean,
    rippleColor: Color,
    content: @Composable BoxScope.() -> Unit,
) {
    val scaledSize = size.withTypographyScale()
    AdaptiveIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(scaledSize),
    ) {
        Box(contentAlignment = Alignment.Center, content = content)
    }
}
