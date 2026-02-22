package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_delete
import com.kevlina.budgetplus.core.theme.LocalAppColors
import org.jetbrains.compose.resources.vectorResource

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    enabled: Boolean = true,
    rippleColor: Color = LocalAppColors.current.light,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        content = content,
        modifier = modifier
            .size(size)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = false,
                    radius = size / 2,
                    color = rippleColor
                )
            )
    )
}

@Preview(showBackground = true)
@Composable
private fun IconButton_Preview() = AppTheme {
    IconButton(onClick = {}) {
        IconButton(
            onClick = { },
            rippleColor = LocalAppColors.current.dark,
            size = 40.dp
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_delete),
                contentDescription = null,
                tint = LocalAppColors.current.dark,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}