package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.mohamedrejeb.calf.ui.button.CupertinoButton
import com.mohamedrejeb.calf.ui.button.CupertinoButtonDefaults

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
    // Use the (non Liquid Glass) Cupertino button so the button stays solid and
    // visible in every state, including disabled (Liquid Glass renders the disabled
    // state nearly invisibly). This mirrors the app's original disabled look
    // (the fill color at 40% alpha).
    val contentColor = LocalAppColors.current.light
    CupertinoButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        contentPadding = contentPadding,
        colors = CupertinoButtonDefaults.filledButtonColors(
            containerColor = color,
            contentColor = contentColor,
            disabledContainerColor = color.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.6f),
        ),
        content = content,
    )
}
