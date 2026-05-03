package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.RecordDbClient
import com.kevlina.budgetplus.core.data.remote.Record

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeRecordDbClient : RecordDbClient {

    val addedRecords = mutableListOf<Record>()
    val setRecords = mutableListOf<Pair<String, Record>>()
    val deletedRecordIds = mutableListOf<String>()

    override suspend fun add(record: Record) {
        addedRecords.add(record)
    }

    override suspend fun set(recordId: String, record: Record) {
        setRecords.add(recordId to record)
    }

    override suspend fun delete(recordId: String) {
        deletedRecordIds.add(recordId)
    }

    override suspend fun queryByBatchAndDate(batchId: String?, fromDate: Int): List<Pair<String, Record>> {
        return emptyList()
    }

    override suspend fun queryByCategory(category: String): List<Pair<String, Record>> {
        return emptyList()
    }
}
