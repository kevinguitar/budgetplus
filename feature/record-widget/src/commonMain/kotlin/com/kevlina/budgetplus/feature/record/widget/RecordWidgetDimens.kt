package com.kevlina.budgetplus.feature.record.widget

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The layout of the record widget, shared by the Glance widget and the in-app preview so they
 * don't drift apart.
 */
internal object RecordWidgetDimens {
    val CornerRadius = 16.dp

    val HorizontalPadding = 16.dp
    val HeaderVerticalPadding = 10.dp
    val HeaderFontSize = 16.sp

    val NameFontSize = 15.sp
    val NamePillsSpacing = 8.dp

    /** The vertical padding of an item when the name and the pills fit in the same row. */
    val SingleRowVerticalPadding = 10.dp

    /** The vertical padding of an item when the pills are moved to the 2nd row. */
    val TwoRowsVerticalPadding = 8.dp
    val TwoRowsSpacing = 4.dp

    val PillFontSize = 12.sp
    val PillHorizontalPadding = 8.dp
    val PillVerticalPadding = 1.dp
    val PillCornerRadius = 8.dp
    val PillsSpacing = 6.dp

    val DividerHeight = 1.dp
}
