package com.kevlina.budgetplus.feature.record.widget

import kotlinx.coroutines.flow.Flow

/**
 * Persists the [RecordWidgetConfig] of each widget instance. The storage is platform specific
 * since the iOS widget extension needs to read it from the shared App Group container.
 */
interface RecordWidgetConfigStore {

    val configs: Flow<List<RecordWidgetConfig>>

    suspend fun get(id: String): RecordWidgetConfig?

    /**
     * Insert the config, or replace the existing one with the same id.
     */
    suspend fun save(config: RecordWidgetConfig)

    suspend fun delete(id: String)

    /**
     * Rename the book in all the configs that are tied to [bookId].
     * @return true if any config was updated.
     */
    suspend fun updateBookName(bookId: String, bookName: String): Boolean
}
