package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors

/**
 * Adaptive clickable surface.
 *
 * On Android it renders a Material3 [androidx.compose.material3.Surface] with a
 * ripple. On iOS it renders the same colored surface but with Calf's native iOS
 * scaling press effect (via [adaptiveClickable]) instead of the ripple.
 */
@Composable
expect fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = LocalAppColors.current.dark,
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
)

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
