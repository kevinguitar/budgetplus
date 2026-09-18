package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.kevlina.budgetplus.core.theme.withTypographyScale

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
    Box(
        contentAlignment = Alignment.Center,
        content = content,
        modifier = modifier
            .size(scaledSize)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = false,
                    radius = scaledSize / 2,
                    color = rippleColor
                )
            )
    )
}
