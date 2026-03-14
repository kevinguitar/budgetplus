package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalTypographyScale
import androidx.compose.material3.Icon as MaterialIcon

@Composable
fun Icon(
    imageVector: ImageVector,
    tint: Color,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    MaterialIcon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(size * LocalTypographyScale.current.scale),
        tint = tint
    )
}