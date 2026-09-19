package com.kevlina.budgetplus.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Surface as MaterialSurface

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

    val box = @Composable {
        Box(
            contentAlignment = Alignment.Center,
            content = content
        )
    }

    if (onLongClick != null) {
        MaterialSurface(
            modifier = modifier
                .clip(shape)
                .combinedClickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
            shape = shape,
            color = surfaceColor,
            border = border,
            shadowElevation = elevation,
            content = box
        )
    } else {
        MaterialSurface(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            color = surfaceColor,
            border = border,
            shadowElevation = elevation,
            interactionSource = interactionSource,
            content = box
        )
    }
}
