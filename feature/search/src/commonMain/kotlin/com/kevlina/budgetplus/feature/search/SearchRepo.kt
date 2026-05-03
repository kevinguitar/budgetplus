package com.kevlina.budgetplus.feature.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.feature.search.ui.SearchCategory
import com.kevlina.budgetplus.feature.search.ui.SearchPeriod
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

@Inject
class SearchRepo(
    private val searchQueryClient: SearchQueryClient,
    private val bookRepo: BookRepo,
    private val snackbarSender: SnackbarSender,
    private val tracker: Tracker,
) : ViewModel() {

    val query = TextFieldState()
    val category = MutableStateFlow<SearchCategory>(SearchCategory.None)
    val period = MutableStateFlow<SearchPeriod>(SearchPeriod.PastMonth)

    private val dbQueryActivationFlow = combine(
        snapshotFlow { query.text },
        category,
        ::Pair
    )
        .filter { (queryText, category) ->
            queryText.isNotBlank() || category != SearchCategory.None
        }
        .map { Unit }
        .distinctUntilChanged()

    val dbResult: Flow<DbResult> = combine(
        period,
        dbQueryActivationFlow,
        ::Pair
    )
        .flatMapLatest { (period, _) ->
            val bookId = bookRepo.currentBookId
            if (bookId.isNullOrEmpty()) {
                flowOf(DbResult.Empty)
            } else {
                queryFromDb(bookId, period)
            }
        }
        // Reply the latest result to avoid querying DB again
        .shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

    private var recordsJob: Job? = null

    fun endConnection() {
        recordsJob?.cancel()
        recordsJob = null
    }

    private fun queryFromDb(
        bookId: String,
        period: SearchPeriod,
    ): Flow<DbResult> {
        val flow = MutableSharedFlow<DbResult>(
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            replay = 1
        )

        Logger.d { "Search: Performing DB query with period $period" }
        flow.tryEmit(DbResult.Loading)

        recordsJob?.cancel()
        recordsJob = searchQueryClient
            .queryRecords(bookId, period.fromDate(), period.untilDate())
            .catch { snackbarSender.sendError(it) }
            .onEach { records ->
                Logger.d { "Search: result size ${records.size}" }
                tracker.logEvent(
                    event = "search_queried_from_db",
                    params = mapOf("db_read_count" to records.size)
                )
                flow.tryEmit(DbResult.Success(records))
            }
            .launchIn(viewModelScope)

        return flow
    }

    private fun SearchPeriod.fromDate(): LocalDate =
        when (this) {
            SearchPeriod.PastMonth -> LocalDate.now().minus(1, DateTimeUnit.MONTH)
            SearchPeriod.PastHalfYear -> LocalDate.now().minus(6, DateTimeUnit.MONTH)
            SearchPeriod.PastYear -> LocalDate.now().minus(1, DateTimeUnit.YEAR)
            is SearchPeriod.Custom -> from
        }

    private fun SearchPeriod.untilDate(): LocalDate =
        when (this) {
            SearchPeriod.PastMonth, SearchPeriod.PastHalfYear, SearchPeriod.PastYear -> LocalDate.now()
            is SearchPeriod.Custom -> until
        }

    sealed interface DbResult {
        data object Empty : DbResult
        data object Loading : DbResult
        data class Success(val records: List<Record>) : DbResult
    }
}