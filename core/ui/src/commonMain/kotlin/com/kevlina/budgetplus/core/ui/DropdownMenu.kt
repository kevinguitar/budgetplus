package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_lock
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import org.jetbrains.compose.resources.vectorResource
import androidx.compose.material3.DropdownMenu as MaterialDropdownMenu
import androidx.compose.material3.DropdownMenuItem as MaterialDropdownMenuItem

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    MaterialDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.background(LocalAppColors.current.light),
        offset = offset,
        properties = properties,
        content = content
    )
}

@Composable
fun DropdownItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    MaterialDropdownMenuItem(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        text = content,
    )
}

@Composable
fun DropdownItem(
    name: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    MaterialDropdownMenuItem(
        onClick = onClick,
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = LocalAppColors.current.dark,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(20.dp)
                    )
                }

                Text(
                    text = name,
                    color = LocalAppColors.current.dark,
                    fontSize = FontSize.SemiLarge
                )
            }
        }
    )
}

@Composable
fun DropdownDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
}

@Preview
@Composable
private fun DropdownMenu_Preview() = AppTheme(themeColors = ThemeColors.Lavender) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = { }
    ) {
        DropdownItem(
            name = "Item 1",
            icon = vectorResource(Res.drawable.ic_lock),
            onClick = {}
        )
        DropdownDivider()
        DropdownItem(
            name = "Item 2",
            onClick = {}
        )
        DropdownItem(
            name = "Item 3",
            onClick = {}
        )
    }
}