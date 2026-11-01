package com.kevlina.budgetplus.feature.record.widget

import com.kevlina.budgetplus.core.theme.ThemeManager
import com.kevlina.budgetplus.core.theme.toHexCode
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * WidgetKit is only available in Swift, the Swift layer registers [reloader] on app start.
 */
object RecordWidgetTimelines {

    var reloader: (() -> Unit)? = null

    fun reload() {
        reloader?.invoke()
    }
}

@ContributesBinding(AppScope::class)
internal class IosRecordWidgetPlatform(
    private val configStore: RecordWidgetConfigStore,
    private val themeManager: ThemeManager,
    private val json: Json,
) : RecordWidgetPlatform {

    /**
     * iOS doesn't allow apps to add widgets, save the config as a preset which the user can
     * pick from the widget's "Edit Widget" menu.
     */
    override suspend fun addWidget(config: RecordWidgetConfig): AddWidgetResult {
        configStore.save(config)
        refreshWidgets()
        return AddWidgetResult.PresetSaved
    }

    override suspend fun refreshWidgets() {
        val colors = themeManager.themeColors.value
        val theme = RecordWidgetTheme(
            light = colors.light.toHexCode(),
            lightBg = colors.lightBg.toHexCode(),
            primary = colors.primary.toHexCode(),
            dark = colors.dark.toHexCode(),
        )
        recordWidgetDefaults.setObject(
            json.encodeToString(RecordWidgetTheme.serializer(), theme),
            forKey = RECORD_WIDGET_THEME_KEY
        )
        RecordWidgetTimelines.reload()
    }
}

/**
 * Snapshot of the theme colors for the widget extension, in hex without the leading `#`.
 */
@Serializable
private data class RecordWidgetTheme(
    val light: String,
    val lightBg: String,
    val primary: String,
    val dark: String,
)
