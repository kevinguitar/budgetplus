package com.kevlina.budgetplus.feature.record.widget

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The widget picker preview shows the sample items in the app's language. Changing the device or
 * the per-app language is a configuration change without restarting the app, and it doesn't
 * change the theme colors, so [RecordWidgetSyncAction] wouldn't update the preview until the next
 * app start. Update it when the locales change, the platform skips it when it's up to date.
 */
@ContributesIntoSet(AppScope::class)
internal class RecordWidgetPreviewLocaleAction(
    private val context: Context,
    private val widgetPlatform: RecordWidgetPlatform,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : AppStartAction {

    override fun onAppStart() {
        var locales = context.resources.configuration.locales
        context.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                // Ignore the other configuration changes, e.g. rotation.
                if (newConfig.locales == locales) return
                locales = newConfig.locales

                appScope.launch {
                    try {
                        widgetPlatform.updatePreview()
                    } catch (e: Exception) {
                        Logger.e(e, "Fail to update the record widget preview after the locale changed")
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = Unit
        })
    }
}
