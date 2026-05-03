package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.RecordsDb
import dev.gitlive.firebase.firestore.CollectionReference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
internal class RecordDbClientImpl(
    @RecordsDb private val recordsDb: () -> CollectionReference,
) : RecordDbClient {

    override suspend fun add(record: Record) {
        recordsDb().add(record)
    }

    override suspend fun set(recordId: String, record: Record) {
        recordsDb().document(recordId).set(record)
    }

    override suspend fun delete(recordId: String) {
        recordsDb().document(recordId).delete()
    }

    override suspend fun queryByBatchAndDate(batchId: String?, fromDate: Int): List<Pair<String, Record>> {
        val snapshot = recordsDb()
            .where { "batchId" equalTo batchId }
            .where { "date" greaterThanOrEqualTo fromDate }
            .get()
        return snapshot.documents.map { doc ->
            doc.id to doc.data<Record>().copy(id = doc.id)
        }
    }

    override suspend fun queryByCategory(category: String): List<Pair<String, Record>> {
        val snapshot = recordsDb()
            .where { "category" equalTo category }
            .get()
        return snapshot.documents.map { doc ->
            doc.id to doc.data<Record>().copy(id = doc.id)
        }
    }
}
