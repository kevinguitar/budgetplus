package com.kevlina.budgetplus.feature.record.widget

import com.kevlina.budgetplus.core.common.Tracker

enum class RecordWidgetAddedSource(val value: String) {
    /** Android: added from the launcher's widget picker. */
    Os("os"),

    /** Android: pinned from the app's settings. */
    Settings("settings"),

    /** iOS: detected on the next app launch, since iOS doesn't notify the app. */
    Ios("ios"),
}

/**
 * Android: an existing widget is reconfigured from the launcher.
 */
fun Tracker.trackRecordWidgetEdited(itemCount: Int, isBookChanged: Boolean, useDynamicColors: Boolean) {
    logEvent(
        event = "record_widget_edited",
        params = mapOf(
            "item_count" to itemCount,
            "book_changed" to isBookChanged,
            "dynamic_colors" to useDynamicColors,
        )
    )
}

fun Tracker.trackRecordWidgetAdded(
    source: RecordWidgetAddedSource,
    itemCount: Int? = null,
    useDynamicColors: Boolean? = null,
) {
    logEvent(
        event = "record_widget_added",
        params = buildMap {
            put("source", source.value)
            if (itemCount != null) put("item_count", itemCount)
            if (useDynamicColors != null) put("dynamic_colors", useDynamicColors)
        }
    )
}
