package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_delete
import com.kevlina.budgetplus.core.theme.LocalAppColors
import org.jetbrains.compose.resources.vectorResource

/**
 * Adaptive icon button.
 *
 * On Android it renders a sized box with a borderless Material ripple
 * (unchanged). On iOS it uses the native iOS scaling press effect instead of the
 * ripple.
 */
@Composable
expect fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    enabled: Boolean = true,
    rippleColor: Color = LocalAppColors.current.light,
    content: @Composable BoxScope.() -> Unit,
)

@Preview(showBackground = true)
@Composable
private fun IconButton_Preview() = AppTheme {
    IconButton(
        onClick = { },
        rippleColor = LocalAppColors.current.dark,
        size = 40.dp
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_delete),
            contentDescription = null,
            tint = LocalAppColors.current.dark,
            size = 20.dp,
        )
    }
}
