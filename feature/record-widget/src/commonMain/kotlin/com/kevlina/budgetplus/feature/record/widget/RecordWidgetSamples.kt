package com.kevlina.budgetplus.feature.record.widget

import com.kevlina.budgetplus.core.common.RecordType

/**
 * The sample widget, in English, for the `@Preview`s. The widget picker preview on Android uses
 * the localized version of the same items, see `Context.sampleRecordWidgetConfig()`.
 */
internal object RecordWidgetSamples {

    val items = listOf(
        RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Morning Coffee"),
        RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Lunch"),
        RecordWidgetItem(type = RecordType.Expense, category = "Transport", name = "Commute"),
        RecordWidgetItem(type = RecordType.Expense, category = "Daily", name = "Groceries"),
        RecordWidgetItem(type = RecordType.Income, category = "Salary", name = "Salary"),
    )

    val config = RecordWidgetConfig(
        id = "sample",
        bookId = "sample",
        bookName = "My Budget",
        items = items,
        createdOn = 0,
    )
}
