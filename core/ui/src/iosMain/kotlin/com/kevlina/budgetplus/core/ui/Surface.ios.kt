package com.kevlina.budgetplus.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

private const val IOS_PRESSED_SCALE = 0.96f

@Composable
actual fun Surface(
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)?,
    enabled: Boolean,
    shape: Shape,
    color: Color,
    border: BorderStroke?,
    elevation: Dp,
    interactionSource: MutableInteractionSource,
    content: @Composable BoxScope.() -> Unit,
) {
    val surfaceColor by animateColorAsState(
        targetValue = if (enabled) color else color.copy(alpha = 0.4F),
        label = "surface_color"
    )

    // Native iOS scaling press effect driven by the interaction source.
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) IOS_PRESSED_SCALE else 1f,
        label = "surface_scale"
    )

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    } else {
        Modifier.clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(color = surfaceColor, shape = shape)
            .clip(shape)
            .then(clickModifier),
        content = content,
    )
}
