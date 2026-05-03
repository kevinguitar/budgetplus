package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.data.remote.Record

interface RecordDbClient {
    suspend fun add(record: Record)
    suspend fun set(recordId: String, record: Record)
    suspend fun delete(recordId: String)
    suspend fun queryByBatchAndDate(batchId: String?, fromDate: Int): List<Pair<String, Record>>
    suspend fun queryByCategory(category: String): List<Pair<String, Record>>
}
