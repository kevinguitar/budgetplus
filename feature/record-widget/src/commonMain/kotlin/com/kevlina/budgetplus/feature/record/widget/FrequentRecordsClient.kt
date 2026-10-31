package com.kevlina.budgetplus.feature.record.widget

import com.kevlina.budgetplus.core.data.remote.BooksDb
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.createdOn
import dev.gitlive.firebase.firestore.CollectionReference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.datetime.LocalDate

interface FrequentRecordsClient {

    /**
     * Query the records of the [bookId] which are recorded on or after [fromDate].
     */
    suspend fun queryRecords(bookId: String, fromDate: LocalDate): List<Record>
}

@ContributesBinding(AppScope::class)
internal class FrequentRecordsClientImpl(
    @BooksDb private val booksDb: Lazy<CollectionReference>,
) : FrequentRecordsClient {

    override suspend fun queryRecords(bookId: String, fromDate: LocalDate): List<Record> {
        return booksDb.value
            .document(bookId)
            .collection("records")
            .where { "date" greaterThanOrEqualTo fromDate.toEpochDays() }
            .get()
            .documents
            .map { doc -> doc.data<Record>().copy(id = doc.id) }
    }
}

const val FREQUENT_RECORDS_DAYS = 30
const val FREQUENT_RECORDS_LIMIT = 20

/**
 * A frequently recorded item and how many times it was recorded in the queried period.
 */
data class FrequentItem(
    val item: RecordWidgetItem,
    val count: Int,
)

/**
 * Group the records by type, category and name, regardless of the price. The most frequently
 * recorded items come first, ties are broken by the most recently recorded one.
 */
internal fun List<Record>.toFrequentItems(limit: Int = FREQUENT_RECORDS_LIMIT): List<FrequentItem> {
    return groupBy { record ->
        RecordWidgetItem(
            type = record.type,
            category = record.category,
            name = record.name.trim().ifEmpty { record.category },
        )
    }
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<RecordWidgetItem, List<Record>>> { it.value.size }
                .thenByDescending { entry -> entry.value.maxOf { it.createdOn } }
        )
        .take(limit)
        .map { FrequentItem(item = it.key, count = it.value.size) }
}
