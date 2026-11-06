package com.kevlina.budgetplus.feature.record.widget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.record_income
import budgetplus.core.common.generated.resources.record_widget_preview_empty
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetDimens
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetItem
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetSamples
import org.jetbrains.compose.resources.stringResource

/**
 * The color roles of the in-app widget preview, the same roles as the Glance widget's colors.
 */
internal data class WidgetPreviewColors(
    val background: Color,
    val headerBackground: Color,
    val headerText: Color,
    val itemText: Color,
    val pillBackground: Color,
    val pillText: Color,
    val divider: Color,
) {
    companion object {
        fun fromTheme(colors: ThemeColors) = WidgetPreviewColors(
            background = colors.light,
            headerBackground = colors.dark,
            headerText = colors.light,
            itemText = colors.dark,
            pillBackground = colors.primary,
            pillText = colors.light,
            divider = colors.lightBg,
        )
    }
}

/**
 * The Material You colors of the widget, null if they aren't supported on the device.
 */
@Composable
internal expect fun rememberDynamicWidgetPreviewColors(): WidgetPreviewColors?

/**
 * A non-interactive replica of the record widget for the settings screen. Glance can't be
 * rendered in the app's UI, and the screen is shared with iOS, so it mirrors the Glance layout
 * with plain Compose.
 */
@Composable
internal fun RecordWidgetPreview(
    bookName: String,
    items: List<RecordWidgetItem>,
    colors: WidgetPreviewColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(RecordWidgetDimens.CornerRadius))
            .background(colors.background)
    ) {
        PreviewText(
            text = bookName,
            color = colors.headerText,
            fontSize = RecordWidgetDimens.HeaderFontSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.headerBackground)
                .padding(
                    horizontal = RecordWidgetDimens.HorizontalPadding,
                    vertical = RecordWidgetDimens.HeaderVerticalPadding,
                )
        )

        if (items.isEmpty()) {
            PreviewText(
                text = stringResource(Res.string.record_widget_preview_empty),
                color = colors.itemText.copy(alpha = 0.6F),
                fontSize = RecordWidgetDimens.NameFontSize,
                modifier = Modifier.padding(
                    horizontal = RecordWidgetDimens.HorizontalPadding,
                    vertical = RecordWidgetDimens.SingleRowVerticalPadding,
                )
            )
        }

        items.forEachIndexed { index, item ->
            PreviewItemRow(item = item, colors = colors)
            if (index != items.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RecordWidgetDimens.DividerHeight)
                        .background(colors.divider)
                )
            }
        }
    }
}

@Composable
private fun PreviewItemRow(item: RecordWidgetItem, colors: WidgetPreviewColors) {
    val pills = buildList {
        add(item.category)
        if (item.type == RecordType.Income) add(stringResource(Res.string.record_income))
    }

    NameAndPillsLayout(
        name = {
            PreviewText(
                text = item.name,
                color = colors.itemText,
                fontSize = RecordWidgetDimens.NameFontSize,
                fontWeight = FontWeight.Medium,
            )
        },
        pills = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RecordWidgetDimens.PillsSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pills.forEach { pill ->
                    Box(
                        modifier = Modifier
                            .background(colors.pillBackground, RoundedCornerShape(RecordWidgetDimens.PillCornerRadius))
                            .padding(
                                horizontal = RecordWidgetDimens.PillHorizontalPadding,
                                vertical = RecordWidgetDimens.PillVerticalPadding,
                            )
                    ) {
                        PreviewText(text = pill, color = colors.pillText, fontSize = RecordWidgetDimens.PillFontSize)
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = RecordWidgetDimens.HorizontalPadding),
    )
}

/**
 * Place the name and the pills in the same row when they fit, otherwise move the pills to the
 * 2nd row, the same rule as the Glance widget.
 */
@Composable
private fun NameAndPillsLayout(
    name: @Composable () -> Unit,
    pills: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf(name, pills),
        modifier = modifier,
    ) { [nameMeasurables, pillsMeasurables], constraints ->
        val nameMeasurable = nameMeasurables.single()
        val pillsPlaceable = pillsMeasurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val spacing = RecordWidgetDimens.NamePillsSpacing.roundToPx()
        val maxWidth = constraints.maxWidth
        val isSingleRow = nameMeasurable.maxIntrinsicWidth(Constraints.Infinity) + spacing + pillsPlaceable.width <= maxWidth

        if (isSingleRow) {
            val verticalPadding = RecordWidgetDimens.SingleRowVerticalPadding.roundToPx()
            val namePlaceable = nameMeasurable.measure(
                Constraints(maxWidth = (maxWidth - spacing - pillsPlaceable.width).coerceAtLeast(0))
            )
            val contentHeight = maxOf(namePlaceable.height, pillsPlaceable.height)
            layout(maxWidth, contentHeight + verticalPadding * 2) {
                namePlaceable.place(0, verticalPadding + (contentHeight - namePlaceable.height) / 2)
                pillsPlaceable.place(
                    namePlaceable.width + spacing,
                    verticalPadding + (contentHeight - pillsPlaceable.height) / 2,
                )
            }
        } else {
            val verticalPadding = RecordWidgetDimens.TwoRowsVerticalPadding.roundToPx()
            val rowSpacing = RecordWidgetDimens.TwoRowsSpacing.roundToPx()
            val namePlaceable = nameMeasurable.measure(Constraints(maxWidth = maxWidth))
            layout(maxWidth, namePlaceable.height + rowSpacing + pillsPlaceable.height + verticalPadding * 2) {
                namePlaceable.place(0, verticalPadding)
                pillsPlaceable.place(0, verticalPadding + namePlaceable.height + rowSpacing)
            }
        }
    }
}

/**
 * Like the widget, the texts aren't impacted by the app's typography scale.
 */
@Composable
private fun PreviewText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    BasicText(
        text = text,
        style = TextStyle(color = color, fontSize = fontSize, fontWeight = fontWeight),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

// The long name moves the pills to the 2nd row.
private val previewItems = listOf(
    RecordWidgetItem(type = RecordType.Expense, category = "Rent", name = "36 Cassia Crescent Apartment Rent"),
) + RecordWidgetSamples.items

@Preview
@Composable
private fun RecordWidgetPreview_Preview() {
    RecordWidgetPreview(
        bookName = "My Budget",
        items = previewItems,
        colors = WidgetPreviewColors.fromTheme(ThemeColors.MilkTea),
    )
}

@Preview
@Composable
private fun RecordWidgetPreviewEmpty_Preview() {
    RecordWidgetPreview(
        bookName = "My Budget",
        items = emptyList(),
        colors = WidgetPreviewColors.fromTheme(ThemeColors.Dusk),
    )
}
