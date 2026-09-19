package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 */
data class DropdownItemModel(
    val name: String,
    val icon: ImageVector? = null,
    /**
     * Optional iOS SF Symbol name used instead of [icon] on iOS. SF Symbols are
     * rendered as template images in the native pull-down menu, so they are
     * automatically tinted to match the menu's label color (unlike a rasterized
     * [ImageVector], which keeps its own color).
     */
    val iosSfSymbol: String? = null,
    val enabled: Boolean = true,
    val isDestructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Adaptive dropdown menu.
 *
 * On Android it renders the themed Material [DropdownMenu] using the [content]
 * composable slot (unchanged). On iOS it renders a native pull-down menu
 * (Calf `AdaptiveDropDown`) built from [iosItems]; the [content] slot is not
 * used on iOS.
 *
 * Call sites should pass both [iosItems] (the data model) and [content] (the
 * Material composables).
 */
@Composable
fun BoxScope.DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    iosItems: List<DropdownItemModel> = emptyList(),
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    AdaptiveDropDown(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.dropdownBackground(LocalAppColors.current.light),
        offset = offset,
        properties = properties,
        containerColor = LocalAppColors.current.light,
        iosItems = iosItems.map { item ->
            AdaptiveDropDownItem(
                title = item.name,
                iosIcon = item.iosSfSymbol?.let { UIKitImage.SystemName(it) }
                    ?: item.icon?.let { UIKitImage.Vector(it) },
                isDestructive = item.isDestructive,
                isDisabled = !item.enabled,
                onClick = item.onClick,
            )
        },
        materialContent = content,
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
                        size = 20.dp,
                        modifier = Modifier.padding(end = 4.dp)
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
