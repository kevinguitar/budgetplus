package com.kevlina.budgetplus.feature.record.widget

import androidx.datastore.preferences.core.stringPreferencesKey
import com.kevlina.budgetplus.core.data.local.Preference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer

/**
 * Widgets on Android run in the app process, so the configs are simply stored in the app's
 * preference. Configs are keyed by the appWidgetId, or a random id while pinning is pending.
 */
@ContributesBinding(AppScope::class)
internal class AndroidRecordWidgetConfigStore(
    private val preference: Preference,
) : RecordWidgetConfigStore {

    private val configsKey = stringPreferencesKey("record_widget_configs")
    private val serializer = ListSerializer(RecordWidgetConfig.serializer())

    override val configs: Flow<List<RecordWidgetConfig>> = preference.of(configsKey, serializer)
        .map { it.orEmpty() }

    override suspend fun get(id: String): RecordWidgetConfig? {
        return configs.first().firstOrNull { it.id == id }
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

    private suspend fun update(transform: (List<RecordWidgetConfig>) -> List<RecordWidgetConfig>) {
        preference.updateTransform(configsKey, serializer) { current ->
            transform(current.orEmpty())
        }
    }
}
