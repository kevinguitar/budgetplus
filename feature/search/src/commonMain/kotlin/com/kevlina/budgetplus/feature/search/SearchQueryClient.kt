package com.kevlina.budgetplus.feature.search

import com.kevlina.budgetplus.core.data.remote.Record
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface SearchQueryClient {
    fun queryRecords(
        bookId: String,
        fromDate: LocalDate,
        untilDate: LocalDate,
    ): Flow<List<Record>>
}
