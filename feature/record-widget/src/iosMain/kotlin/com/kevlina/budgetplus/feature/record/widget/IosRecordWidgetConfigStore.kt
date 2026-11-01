package com.kevlina.budgetplus.feature.record.widget

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

internal const val RECORD_WIDGET_CONFIGS_KEY = "record_widget_configs"
internal const val RECORD_WIDGET_THEME_KEY = "record_widget_theme"

/**
 * The App Group shared with the widget extension is `group.<app bundle id>`, so the stage and
 * production builds don't share data. Keep it in sync with `APP_GROUP_ID` in the Xcode project
 * and `RecordWidgetStore.swift`.
 */
internal val recordWidgetDefaults: NSUserDefaults by lazy {
    NSUserDefaults(suiteName = "group.${NSBundle.mainBundle.bundleIdentifier}")
}

/**
 * The configs (presets) are stored as JSON in the App Group user defaults, so the widget
 * extension can read them without linking the shared framework.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class IosRecordWidgetConfigStore(
    private val json: Json,
) : RecordWidgetConfigStore {

    private val serializer = ListSerializer(RecordWidgetConfig.serializer())
    private val mutex = Mutex()

    override val configs: StateFlow<List<RecordWidgetConfig>>
        field = MutableStateFlow(readConfigs())

    override suspend fun get(id: String): RecordWidgetConfig? {
        return configs.value.firstOrNull { it.id == id }
    }

    override suspend fun save(config: RecordWidgetConfig) {
        update { configs -> configs.filterNot { it.id == config.id } + config }
    }

    override suspend fun delete(id: String) {
        update { configs -> configs.filterNot { it.id == id } }
    }

    override suspend fun updateBookName(bookId: String, bookName: String): Boolean {
        var isUpdated = false
        update { configs ->
            configs.map { config ->
                if (config.bookId == bookId && config.bookName != bookName) {
                    isUpdated = true
                    config.copy(bookName = bookName)
                } else {
                    config
                }
            }
        }
        return isUpdated
    }

    private fun readConfigs(): List<RecordWidgetConfig> {
        val raw = recordWidgetDefaults.stringForKey(RECORD_WIDGET_CONFIGS_KEY) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    private suspend fun update(transform: (List<RecordWidgetConfig>) -> List<RecordWidgetConfig>) {
        mutex.withLock {
            val newConfigs = transform(configs.value)
            if (newConfigs == configs.value) return
            recordWidgetDefaults.setObject(
                json.encodeToString(serializer, newConfigs),
                forKey = RECORD_WIDGET_CONFIGS_KEY
            )
            configs.value = newConfigs
        }
    }
}
