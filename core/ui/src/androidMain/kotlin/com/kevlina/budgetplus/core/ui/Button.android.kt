package com.kevlina.budgetplus.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.Surface as MaterialSurface

@Composable
actual fun Button(
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    color: Color,
    shape: Shape,
    contentPadding: PaddingValues,
    content: @Composable RowScope.() -> Unit,
) {
    val buttonColor by animateColorAsState(
        targetValue = if (enabled) color else color.copy(alpha = 0.4F),
        label = "button_color"
    )

    MaterialSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = buttonColor,
        interactionSource = remember { MutableInteractionSource() },
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
