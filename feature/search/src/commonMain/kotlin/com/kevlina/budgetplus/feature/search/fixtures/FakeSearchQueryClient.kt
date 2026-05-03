package com.kevlina.budgetplus.feature.search.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.feature.search.SearchQueryClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeSearchQueryClient : SearchQueryClient {

    val queryCalls = mutableListOf<Triple<String, Int, Int>>()
    val recordsFlow = MutableSharedFlow<List<Record>>()

    override fun queryRecords(bookId: String, fromDate: Int, untilDate: Int): Flow<List<Record>> {
        queryCalls.add(Triple(bookId, fromDate, untilDate))
        return recordsFlow
    }
}
