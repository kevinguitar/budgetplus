package com.kevlina.budgetplus.feature.search

import com.kevlina.budgetplus.core.data.remote.BooksDb
import com.kevlina.budgetplus.core.data.remote.Record
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Direction
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

@ContributesBinding(AppScope::class)
internal class SearchQueryClientImpl(
    @BooksDb private val booksDb: Lazy<CollectionReference>,
) : SearchQueryClient {

    override fun queryRecords(bookId: String, fromDate: LocalDate, untilDate: LocalDate): Flow<List<Record>> {
        return booksDb.value
            .document(bookId)
            .collection("records")
            .orderBy("date", Direction.DESCENDING)
            .where { "date" greaterThanOrEqualTo fromDate.toEpochDays() }
            .where { "date" lessThanOrEqualTo untilDate.toEpochDays() }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc -> doc.data<Record>().copy(id = doc.id) }
            }
    }
}
