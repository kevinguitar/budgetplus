package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

@Composable
actual fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    iosItems: List<DropdownItemModel>,
    modifier: Modifier,
    offset: DpOffset,
    properties: PopupProperties,
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

@Preview
@Composable
private fun DropdownMenu_Preview() = AppTheme(themeColors = ThemeColors.Dusk) {
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
