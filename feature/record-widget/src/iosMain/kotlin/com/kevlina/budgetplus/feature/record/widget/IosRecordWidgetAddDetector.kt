package com.kevlina.budgetplus.feature.record.widget

import com.kevlina.budgetplus.core.common.Tracker
import dev.zacsweers.metro.Inject

/**
 * iOS doesn't notify the app when a widget is added, so the Swift layer reports the number of
 * record widgets on the home screen whenever the app becomes active. An increase means widgets
 * were added since the last check.
 */
@Inject
class IosRecordWidgetAddDetector(
    private val tracker: Tracker,
) {

    fun onWidgetCountChanged(count: Int) {
        val defaults = recordWidgetDefaults
        val lastCount = defaults.integerForKey(LAST_WIDGET_COUNT_KEY).toInt()
        repeat((count - lastCount).coerceAtLeast(0)) {
            tracker.trackRecordWidgetAdded(source = RecordWidgetAddedSource.Ios)
        }
        defaults.setInteger(count.toLong(), forKey = LAST_WIDGET_COUNT_KEY)
    }

    private companion object {
        const val LAST_WIDGET_COUNT_KEY = "record_widget_last_count"
    }
}
