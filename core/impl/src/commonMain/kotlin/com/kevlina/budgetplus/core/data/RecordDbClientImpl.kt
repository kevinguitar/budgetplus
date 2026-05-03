package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.RecordsDb
import dev.gitlive.firebase.firestore.CollectionReference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
internal class RecordDbClientImpl(
    @RecordsDb private val recordsDb: Lazy<CollectionReference>,
) : RecordDbClient {

    override suspend fun add(record: Record) {
        recordsDb.value.add(record)
    }

    override suspend fun set(recordId: String, record: Record) {
        recordsDb.value.document(recordId).set(record)
    }

    override suspend fun delete(recordId: String) {
        recordsDb.value.document(recordId).delete()
    }

    override suspend fun queryByBatchAndDate(batchId: String?, fromDate: Long): List<Pair<String, Record>> {
        val snapshot = recordsDb.value
            .where { "batchId" equalTo batchId }
            .where { "date" greaterThanOrEqualTo fromDate }
            .get()
        return snapshot.documents.map { doc ->
            doc.id to doc.data<Record>().copy(id = doc.id)
        }
    }

    override suspend fun queryByCategory(category: String): List<Pair<String, Record>> {
        val snapshot = recordsDb.value
            .where { "category" equalTo category }
            .get()
        return snapshot.documents.map { doc ->
            doc.id to doc.data<Record>().copy(id = doc.id)
        }
    }
}
