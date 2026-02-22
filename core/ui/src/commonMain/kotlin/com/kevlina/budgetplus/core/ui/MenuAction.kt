package com.kevlina.budgetplus.core.ui

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_edit_note
import com.kevlina.budgetplus.core.theme.LocalAppColors
import org.jetbrains.compose.resources.vectorResource

private const val DISABLED_ALPHA = 0.5F

@Composable
fun MenuAction(
    imageVector: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    imageTintColor: Color = LocalAppColors.current.light,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = description,
            tint = imageTintColor,
            modifier = Modifier.thenIf(!enabled) {
                Modifier.alpha(DISABLED_ALPHA)
            }
        )
    }
}

@Preview
@Composable
private fun MenuAction_Preview() = AppTheme {
    MenuAction(imageVector = vectorResource(Res.drawable.ic_edit_note), description = "")
}