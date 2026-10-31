package com.kevlina.budgetplus.feature.record.widget

import com.kevlina.budgetplus.feature.record.widget.config.RecordWidgetConfigActivity
import com.kevlina.budgetplus.feature.record.widget.glance.RecordGlanceWidget
import com.kevlina.budgetplus.feature.record.widget.glance.RecordWidgetPinnedReceiver
import com.kevlina.budgetplus.feature.record.widget.glance.RecordWidgetReceiver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension

/**
 * The widget components are instantiated by the system, so they inject their dependencies via
 * this graph extension.
 */
@GraphExtension
interface RecordWidgetGraph {

    fun inject(target: RecordWidgetConfigActivity)

    fun inject(target: RecordWidgetReceiver)

    fun inject(target: RecordWidgetPinnedReceiver)

    fun inject(target: RecordGlanceWidget)

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    fun interface Factory {
        fun createRecordWidgetGraph(): RecordWidgetGraph
    }
}
