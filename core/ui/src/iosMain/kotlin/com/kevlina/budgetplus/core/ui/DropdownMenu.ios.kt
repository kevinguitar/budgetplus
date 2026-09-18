package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.window.PopupProperties
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.mohamedrejeb.calf.ui.dropdown.AdaptiveDropDown
import com.mohamedrejeb.calf.ui.dropdown.AdaptiveDropDownItem
import com.mohamedrejeb.calf.ui.uikit.UIKitImage

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
    // AdaptiveDropDown requires a BoxScope to anchor the native pull-down menu.
    // The Box is placed where the DropdownMenu sits (next to its trigger), which
    // mirrors how the Material popup anchors on Android.
    Box {
        AdaptiveDropDown(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            offset = offset,
            properties = properties,
            containerColor = LocalAppColors.current.light,
            iosItems = iosItems.map { item ->
                AdaptiveDropDownItem(
                    title = item.name,
                    iosIcon = item.icon?.let { UIKitImage.Vector(it) },
                    isDestructive = item.isDestructive,
                    isDisabled = !item.enabled,
                    onClick = item.onClick,
                )
            },
            materialContent = content,
        )
    }
}
