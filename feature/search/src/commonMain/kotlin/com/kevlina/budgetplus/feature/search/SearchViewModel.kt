package com.kevlina.budgetplus.feature.search

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.di.AssistedFactoryKey
import com.kevlina.budgetplus.core.common.di.ViewModelAssistedFactory
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.RecordRepo
import com.kevlina.budgetplus.core.data.UserRepo
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.feature.category.pills.CategoriesViewModel
import com.kevlina.budgetplus.feature.category.pills.toState
import com.kevlina.budgetplus.feature.record.card.RecordCardState
import com.kevlina.budgetplus.feature.search.ui.SearchCategory
import com.kevlina.budgetplus.feature.search.ui.SearchFilterState
import com.kevlina.budgetplus.feature.search.ui.SearchPeriod
import com.kevlina.budgetplus.feature.search.ui.SearchPeriod.Companion.requiresPremium
import com.kevlina.budgetplus.feature.search.ui.SearchResult
import com.kevlina.budgetplus.feature.search.ui.SearchResultState
import com.kevlina.budgetplus.feature.search.ui.SearchState
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@AssistedInject
class SearchViewModel(
    @Assisted params: BookDest.Search,
    private val searchRepo: SearchRepo,
    private val authManager: AuthManager,
    private val bookRepo: BookRepo,
    private val recordRepo: RecordRepo,
    private val userRepo: UserRepo,
    private val tracker: Tracker,
    val navController: NavController<BookDest>,
    categoriesVm: CategoriesViewModel,
) : ViewModel() {

    private val type = MutableStateFlow(params.type)
    private val author = MutableStateFlow<User?>(null)

    private val searchResult = combine(
        searchRepo.dbResult,
        snapshotFlow { searchRepo.query.text },
        type,
        searchRepo.category,
        author
    ) { dbResult, query, type, category, author ->
        when (dbResult) {
            SearchRepo.DbResult.Empty -> SearchResult.Empty
            SearchRepo.DbResult.Loading -> SearchResult.Loading
            is SearchRepo.DbResult.Success -> withContext(Dispatchers.Default) {
                val localSearchResult = dbResult.records
                    .filter { record ->
                        var include = true
                        if (!deepContains(record.name, query) && query !in record.category) {
                            include = false
                        }
                        if (record.type != type) {
                            include = false
                        }
                        if (category is SearchCategory.Selected && record.category != category.name) {
                            include = false
                        }
                        if (author != null && record.author?.id != author.id) {
                            include = false
                        }
                        include
                    }

                if (localSearchResult.isEmpty()) {
                    SearchResult.Empty
                } else {
                    SearchResult.Success(
                        records = localSearchResult.mapToStates()
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SearchResult.Empty)

    private val editRecordEvent = MutableEventFlow<Record>()
    private val deleteRecordEvent = MutableEventFlow<Record>()
    private val unlockPremiumEvent = MutableEventFlow<Unit>()

    private val allAuthors = bookRepo.bookState
        .map {
            withContext(Dispatchers.Default) {
                it?.authors
                    .orEmpty()
                    .mapNotNull(userRepo::getUser)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val state = SearchState(
        query = searchRepo.query,
        filter = SearchFilterState(
            type = type,
            selectType = {
                if (type.value != it) {
                    type.value = it
                    // Reset the category if the type changes
                    searchRepo.category.value = SearchCategory.None
                }
            },
            category = searchRepo.category,
            categoryGrid = categoriesVm.toState(
                type = type,
                selectedCategory = searchRepo.category.mapState { it.name },
                onCategorySelected = {
                    searchRepo.category.value = if (it == searchRepo.category.value.name) {
                        SearchCategory.None
                    } else {
                        SearchCategory.Selected(it)
                    }
                }
            ),
            selectCategory = { searchRepo.category.value = it },
            period = searchRepo.period,
            selectPeriod = ::selectPeriod,
            author = author,
            allAuthors = allAuthors,
            selectAuthor = { author.value = it },
        ),
        result = SearchResultState(
            result = searchResult,
            editRecordEvent = editRecordEvent,
            deleteRecordEvent = deleteRecordEvent
        ),
    )

    private fun canEditRecord(record: Record): Boolean {
        return bookRepo.canEdit || record.author?.id == authManager.userId
    }

    private fun selectPeriod(period: SearchPeriod) {
        if (period.requiresPremium && !authManager.isPremium.value) {
            navController.navigate(BookDest.UnlockPremium)
            tracker.logEvent("search_period_unlock_premium")
            return
        }
        searchRepo.period.value = period
    }

    private fun List<Record>.mapToStates(): List<RecordCardState> =
        mapIndexed { i, record ->
            RecordCardState(
                item = record,
                formattedPrice = bookRepo.formatPrice(record.price),
                isLast = i == lastIndex,
                canEdit = canEditRecord(record),
                showCategory = true,
                showAuthor = true,
                onEdit = { editRecordEvent.sendEvent(record) },
                onDuplicate = { recordRepo.duplicateRecord(record) },
                onDelete = { deleteRecordEvent.sendEvent(record) }
            )
        }

    override fun onCleared() {
        searchRepo.endConnection()
    }

    @AssistedFactory
    @AssistedFactoryKey(Factory::class)
    @ContributesIntoMap(ViewModelScope::class)
    fun interface Factory : ViewModelAssistedFactory {
        fun create(params: BookDest.Search): SearchViewModel
    }
}