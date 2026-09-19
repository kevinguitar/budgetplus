package com.kevlina.budgetplus.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.kevlina.budgetplus.core.theme.withTypographyScale

private const val IOS_PRESSED_SCALE = 0.9f

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
    val interactionSource = remember { MutableInteractionSource() }

    // Native iOS scaling press effect instead of a ripple.
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) IOS_PRESSED_SCALE else 1f,
        label = "icon_button_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        content = content,
        modifier = modifier
            .size(scaledSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
            )
    )
}
