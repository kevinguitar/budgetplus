package com.kevlina.budgetplus.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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

/**
 *  The default tint of [MenuAction], when it's placed on a background other than the top bar, e.g.
 *  the light side bar on the iPhone Duo. Null to use the light color of the top bar.
 */
val LocalMenuActionTint = staticCompositionLocalOf<Color?> { null }

@Composable
fun MenuAction(
    imageVector: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    imageTintColor: Color = LocalMenuActionTint.current ?: LocalAppColors.current.light,
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