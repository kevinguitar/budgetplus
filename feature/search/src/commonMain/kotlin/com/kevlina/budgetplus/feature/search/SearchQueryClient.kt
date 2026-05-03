package com.kevlina.budgetplus.feature.search

import com.kevlina.budgetplus.core.data.remote.Record
import kotlinx.coroutines.flow.Flow

interface SearchQueryClient {
    fun queryRecords(bookId: String, fromDate: Int, untilDate: Int): Flow<List<Record>>
}
