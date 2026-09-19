package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.mohamedrejeb.calf.ui.dropdown.AdaptiveDropDown
import com.mohamedrejeb.calf.ui.dropdown.AdaptiveDropDownItem
import com.mohamedrejeb.calf.ui.uikit.UIKitImage
import androidx.compose.material3.DropdownMenuItem as MaterialDropdownMenuItem

/**
 * Applies the dropdown background on Android only. On iOS the native pull-down
 * menu handles its own background, and applying it here breaks the layout.
 */
internal expect fun Modifier.dropdownBackground(color: Color): Modifier

/**
 * A declarative description of a single dropdown entry.
 *
 * This model is shared by both platforms: on Android it drives the themed
 * Material [DropdownMenu], and on iOS it is mapped to a native pull-down menu
 * item (Calf `AdaptiveDropDownItem`).
 *
 * Use [DropdownItemModel.Divider] as a list entry to insert a separator between
 * groups of items.
 */
data class DropdownItemModel(
    val name: String,
    val leadingIcon: ImageVector? = null,
    /**
     * Optional icon rendered after [name]. This is Android-only: the native iOS
     * pull-down menu has a single icon slot, so only [leadingIcon]/[iosSfSymbol]
     * are shown there.
     */
    val trailingIcon: ImageVector? = null,
    /**
     * Optional iOS SF Symbol name used instead of [leadingIcon] on iOS. SF
     * Symbols are rendered as template images in the native pull-down menu, so
     * they are automatically tinted to match the menu's label color (unlike a
     * rasterized [leadingIcon], which keeps its own color).
     */
    val iosSfSymbol: String? = null,
    val enabled: Boolean = true,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit,
) {
    internal val isDivider: Boolean get() = this === Divider

    companion object {
        /**
         * A sentinel entry that renders a horizontal divider when placed in the
         * dropdown item list. On iOS the native menu draws its own separators,
         * so this entry is ignored there.
         */
        val Divider: DropdownItemModel = DropdownItemModel(name = "", onClick = {})
    }
}

/**
 * Adaptive dropdown menu.
 *
 * The menu content is fully described by [items] on both platforms. On Android
 * it renders a themed Material [DropdownMenu]; on iOS it renders a native
 * pull-down menu (Calf `AdaptiveDropDown`).
 */
@Composable
fun BoxScope.DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<DropdownItemModel>,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
) {
    AdaptiveDropDown(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.dropdownBackground(LocalAppColors.current.light),
        offset = offset,
        properties = properties,
        containerColor = LocalAppColors.current.light,
        iosItems = items
            .filterNot { it.isDivider }
            .map { item ->
                AdaptiveDropDownItem(
                    title = item.name,
                    iosIcon = item.iosSfSymbol?.let { UIKitImage.SystemName(it) }
                        ?: item.leadingIcon?.let { UIKitImage.Vector(it) },
                    isDestructive = item.isDestructive,
                    isDisabled = !item.enabled,
                    onClick = item.onClick,
                )
            },
        materialContent = {
            items.forEach { item ->
                if (item.isDivider) {
                    DropdownDivider()
                } else {
                    DropdownItem(item)
                }
            }
        },
    )
}

@Composable
private fun DropdownItem(item: DropdownItemModel) {
    MaterialDropdownMenuItem(
        onClick = item.onClick,
        enabled = item.enabled,
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.leadingIcon != null) {
                    Icon(
                        imageVector = item.leadingIcon,
                        contentDescription = null,
                        tint = LocalAppColors.current.dark,
                        size = 20.dp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                Text(
                    text = item.name,
                    color = LocalAppColors.current.dark,
                    fontSize = FontSize.SemiLarge
                )

                if (item.trailingIcon != null) {
                    Icon(
                        imageVector = item.trailingIcon,
                        contentDescription = null,
                        tint = LocalAppColors.current.dark,
                        size = 20.dp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun DropdownDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp))
}
