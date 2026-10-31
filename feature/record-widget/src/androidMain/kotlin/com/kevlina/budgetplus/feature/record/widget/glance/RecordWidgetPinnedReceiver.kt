package com.kevlina.budgetplus.feature.record.widget.glance

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.Logger
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.di.resolveGraphExtensionFactory
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetAddedSource
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetConfigStore
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetGraph
import com.kevlina.budgetplus.feature.record.widget.trackRecordWidgetAdded
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Receives the callback when the user confirms pinning the widget which was requested from the
 * app's settings, then ties the pending config to the new appWidgetId.
 */
class RecordWidgetPinnedReceiver : BroadcastReceiver() {

    @Inject private lateinit var configStore: RecordWidgetConfigStore
    @Inject private lateinit var tracker: Tracker
    @Inject @AppCoroutineScope private lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pendingConfigId = intent.getStringExtra(EXTRA_PENDING_CONFIG_ID) ?: return
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        context.resolveGraphExtensionFactory<RecordWidgetGraph.Factory>()
            .createRecordWidgetGraph()
            .inject(this)

        val pendingResult = goAsync()
        appScope.launch {
            try {
                val config = configStore.get(pendingConfigId)
                if (config == null) {
                    Logger.w("RecordWidget: pending config $pendingConfigId not found")
                    return@launch
                }
                configStore.save(config.copy(id = appWidgetId.toString()))
                configStore.delete(pendingConfigId)

                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                RecordGlanceWidget().update(context, glanceId)
                tracker.trackRecordWidgetAdded(
                    source = RecordWidgetAddedSource.Settings,
                    itemCount = config.items.size,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PENDING_CONFIG_ID = "pending_config_id"
    }
}
