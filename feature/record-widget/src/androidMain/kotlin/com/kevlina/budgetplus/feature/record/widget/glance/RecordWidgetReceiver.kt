package com.kevlina.budgetplus.feature.record.widget.glance

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
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
        launchAsync(context) {
            appWidgetIds.forEach { configStore.delete(it.toString()) }
        }
    }

    /**
     * The widgets get new ids when they're restored from a backup, move the configs to the new ids.
     */
    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        launchAsync(context) {
            // Read them all first, an old id could be reused as the new id of another widget.
            val restoredConfigs = oldWidgetIds.zip(newWidgetIds).mapNotNull { [oldId, newId] ->
                configStore.get(oldId.toString())?.copy(id = newId.toString())
            }
            oldWidgetIds.forEach { configStore.delete(it.toString()) }
            restoredConfigs.forEach { configStore.save(it) }
            glanceAppWidget.updateAll(context)
        }
    }

    private fun launchAsync(context: Context, block: suspend () -> Unit) {
        context.resolveGraphExtensionFactory<RecordWidgetGraph.Factory>()
            .createRecordWidgetGraph()
            .inject(this)

        // goAsync() returns null if it was already called for this broadcast, which is the case for
        // onDeleted since GlanceAppWidgetReceiver.onDeleted calls goAsync() itself.
        val pendingResult: PendingResult? = goAsync()
        appScope.launch {
            try {
                block()
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
