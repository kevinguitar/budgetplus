package com.kevlina.budgetplus.feature.record.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.kevlina.budgetplus.feature.record.widget.glance.RecordGlanceWidget
import com.kevlina.budgetplus.feature.record.widget.glance.RecordWidgetPinnedReceiver
import com.kevlina.budgetplus.feature.record.widget.glance.RecordWidgetReceiver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.first

@ContributesBinding(AppScope::class)
internal class AndroidRecordWidgetPlatform(
    private val context: Context,
    private val configStore: RecordWidgetConfigStore,
) : RecordWidgetPlatform {

    override suspend fun addWidget(config: RecordWidgetConfig): AddWidgetResult {
        val widgetManager = AppWidgetManager.getInstance(context)
        if (!widgetManager.isRequestPinAppWidgetSupported) {
            return AddWidgetResult.PinUnsupported
        }

        // Clean up the configs of the previous pin requests which were never confirmed.
        configStore.configs.first()
            .filter { it.id.toIntOrNull() == null }
            .forEach { configStore.delete(it.id) }

        // The config is saved as pending, and re-keyed with the appWidgetId once the user
        // confirms the pin in RecordWidgetPinnedReceiver.
        configStore.save(config)

        val callback = PendingIntent.getBroadcast(
            context,
            config.id.hashCode(),
            Intent(context, RecordWidgetPinnedReceiver::class.java)
                .putExtra(RecordWidgetPinnedReceiver.EXTRA_PENDING_CONFIG_ID, config.id),
            // Mutable since the launcher fills in the EXTRA_APPWIDGET_ID.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val isRequested = widgetManager.requestPinAppWidget(
            ComponentName(context, RecordWidgetReceiver::class.java),
            null,
            callback
        )
        return if (isRequested) AddWidgetResult.PinRequested else AddWidgetResult.PinUnsupported
    }

    override suspend fun refreshWidgets() {
        RecordGlanceWidget().updateAll(context)
    }
}
