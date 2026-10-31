package com.kevlina.budgetplus.feature.record.widget

import com.kevlina.budgetplus.core.common.RecordType
import kotlinx.serialization.Serializable

const val RECORD_WIDGET_MIN_ITEMS = 1
const val RECORD_WIDGET_MAX_ITEMS = 5

/**
 * A frequently recorded item that can be pinned to the record widget. Price is intentionally
 * excluded since the user fills it in every time.
 */
@Serializable
data class RecordWidgetItem(
    val type: RecordType,
    val category: String,
    val name: String,
)

/**
 * The configuration of a single widget instance. A widget is tied to the book it was created
 * with, and it won't be impacted when the user switches books in the app.
 *
 * Note that this model is also decoded by the iOS widget extension, keep the Swift
 * `RecordWidgetConfig` in sync when changing it.
 *
 * @param id Android: the appWidgetId, iOS: the preset id.
 * @param bookName Snapshot of the book name for the widget header.
 */
@Serializable
data class RecordWidgetConfig(
    val id: String,
    val bookId: String,
    val bookName: String,
    val items: List<RecordWidgetItem>,
    val createdOn: Long,
    val version: Int = CONFIG_VERSION,
) {
    companion object {
        const val CONFIG_VERSION = 1
    }
}
