package com.kevlina.budgetplus.feature.record.widget.glance

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.di.resolveGraphExtensionFactory
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetConfigStore
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetGraph
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RecordWidgetReceiver : GlanceAppWidgetReceiver() {

    @Inject private lateinit var configStore: RecordWidgetConfigStore
    @Inject @AppCoroutineScope private lateinit var appScope: CoroutineScope

    override val glanceAppWidget: GlanceAppWidget = RecordGlanceWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        context.resolveGraphExtensionFactory<RecordWidgetGraph.Factory>()
            .createRecordWidgetGraph()
            .inject(this)

        val pendingResult = goAsync()
        appScope.launch {
            try {
                appWidgetIds.forEach { configStore.delete(it.toString()) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
