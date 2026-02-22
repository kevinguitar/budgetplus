package com.kevlina.budgetplus.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors
import androidx.compose.material3.Surface as MaterialSurface

@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = LocalAppColors.current.dark,
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) {
    val surfaceColor by animateColorAsState(
        targetValue = if (enabled) color else color.copy(alpha = 0.4F),
        label = "surface_color"
    )

    MaterialSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = surfaceColor,
        border = border,
        shadowElevation = elevation,
        interactionSource = interactionSource,
        content = {
            Box(
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    )
}

@Preview
@Composable
private fun Surface_Preview() = AppTheme {
    Surface(
        shape = CircleShape,
        onClick = {},
        modifier = Modifier.heightIn(min = AppTheme.minSurfaceSize)
    ) {
        Text(
            text = "Surface",
            color = LocalAppColors.current.light,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
        )
    }
}