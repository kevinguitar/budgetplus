package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors

/**
 * Adaptive button.
 *
 * On Android it renders a Material3 [androidx.compose.material3.Surface] button
 * filled with [color]. On iOS it renders Calf's native Cupertino button tinted
 * with the same [color].
 */
@Composable
expect fun Button(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = LocalAppColors.current.dark,
    shape: Shape = RoundedCornerShape(12.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit,
)

@Preview
@Composable
private fun Button_Preview() = AppTheme {
    Button(onClick = {}) {
        Text(
            text = "Done",
            color = LocalAppColors.current.light,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
private fun Button_Disabled_Preview() = AppTheme {
    Button(onClick = {}, enabled = false) {
        Text(
            text = "Done",
            color = LocalAppColors.current.light,
            fontWeight = FontWeight.Medium
        )
    }
}
