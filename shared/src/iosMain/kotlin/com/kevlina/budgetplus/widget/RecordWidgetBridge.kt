package com.kevlina.budgetplus.widget

import com.kevlina.budgetplus.book.BudgetPlusIosAppGraphHolder
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetTimelines

/**
 * Bridge between the record widget (WidgetKit, Swift only) and Kotlin, called from iOSApp.swift.
 */
@Suppress("unused") // Called from iOSApp.swift
object RecordWidgetBridge {

    /**
     * Register how to reload the widget timelines, i.e. `WidgetCenter.shared.reloadAllTimelines()`.
     */
    fun setTimelineReloader(reloader: () -> Unit) {
        RecordWidgetTimelines.reloader = reloader
    }

    /**
     * Report the number of record widgets on the home screen, used to detect newly added widgets.
     */
    fun onWidgetCountChanged(count: Int) {
        BudgetPlusIosAppGraphHolder.graph.recordWidgetAddDetector.onWidgetCountChanged(count)
    }
}
