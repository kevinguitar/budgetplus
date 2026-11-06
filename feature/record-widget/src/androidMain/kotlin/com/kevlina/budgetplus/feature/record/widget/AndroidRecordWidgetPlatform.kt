package com.kevlina.budgetplus.feature.record.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.collection.intSetOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.kevlina.budgetplus.core.common.Logger
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.theme.ThemeManager
import com.kevlina.budgetplus.core.theme.toHexCode
import com.kevlina.budgetplus.feature.record.widget.glance.RecordGlanceWidget
import com.kevlina.budgetplus.feature.record.widget.glance.RecordWidgetPinnedReceiver
import com.kevlina.budgetplus.feature.record.widget.glance.RecordWidgetReceiver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
internal class AndroidRecordWidgetPlatform(
    private val context: Context,
    private val configStore: RecordWidgetConfigStore,
    private val themeManager: ThemeManager,
    private val preference: Preference,
) : RecordWidgetPlatform {

    private val previewFingerprintKey = stringPreferencesKey("record_widget_preview_fingerprint")

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

    /**
     * The preview shows the sample items in the app's theme and language, so it's outdated when
     * either of them or the app version changes. The platform rate limits `setWidgetPreview`, so
     * skip it when nothing changed. When it's rate limited, the fingerprint isn't saved and the
     * next app start retries.
     */
    override suspend fun updatePreview() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

        val fingerprint = previewFingerprint()
        if (preference.of(previewFingerprintKey).first() == fingerprint) return

        val result = withContext(Dispatchers.Default) {
            GlanceAppWidgetManager(context).setWidgetPreviews(
                receiver = RecordWidgetReceiver::class,
                // The widget is only available on the home screen, one call for the rate limit.
                widgetCategories = intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN),
            )
        }
        if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
            preference.update(previewFingerprintKey, fingerprint)
        } else {
            Logger.w("RecordWidget: the preview update is rate limited")
        }
    }

    private fun previewFingerprint(): String {
        val colors = themeManager.themeColors.value
        val locale = context.resources.configuration.locales[0].toLanguageTag()
        val versionCode = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        return listOf(
            colors.light.toHexCode(),
            colors.lightBg.toHexCode(),
            colors.primary.toHexCode(),
            colors.dark.toHexCode(),
            locale,
            versionCode,
        ).joinToString("|")
    }
}
