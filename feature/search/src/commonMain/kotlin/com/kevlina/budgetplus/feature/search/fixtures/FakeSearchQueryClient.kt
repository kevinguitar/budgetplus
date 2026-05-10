package com.kevlina.budgetplus.feature.search.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.feature.search.SearchQueryClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.LocalDate

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeSearchQueryClient : SearchQueryClient {

    val queryCalls = mutableListOf<Triple<String, LocalDate, LocalDate>>()
    val recordsFlow = MutableSharedFlow<List<Record>>()

    override fun queryRecords(bookId: String, fromDate: LocalDate, untilDate: LocalDate): Flow<List<Record>> {
        queryCalls.add(Triple(bookId, fromDate, untilDate))
        return recordsFlow
    }
}
