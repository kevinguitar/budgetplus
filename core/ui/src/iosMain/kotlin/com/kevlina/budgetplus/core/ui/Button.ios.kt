package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.mohamedrejeb.calf.ui.button.AdaptiveButton

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
    AdaptiveButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = contentPadding,
        content = content,
    )
}
