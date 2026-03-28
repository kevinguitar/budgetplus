package com.kevlina.budgetplus.feature.records

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.combineState
import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.RecordRepo
import com.kevlina.budgetplus.core.data.RecordsObserver
import com.kevlina.budgetplus.core.data.UserRepo
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.createdOn
import com.kevlina.budgetplus.core.data.resolveAuthor
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.bubble.BubbleRepo
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.ViewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AssistedInject
class RecordsViewModel(
    @Assisted private val params: BookDest.Records,
    val navController: NavController<BookDest>,
    val bookRepo: BookRepo,
    private val userRepo: UserRepo,
    private val recordRepo: RecordRepo,
    private val bubbleRepo: BubbleRepo,
    private val tracker: Tracker,
    private val authManager: AuthManager,
    private val preference: Preference,
    recordsObserver: RecordsObserver,
) : ViewModel() {

    private val sortModeKey = stringPreferencesKey("sortModeCache")
    val sortMode: StateFlow<RecordsSortMode> = preference.of(
        key = sortModeKey, serializer = RecordsSortMode.serializer(), default = RecordsSortMode.Date, scope = viewModelScope
    )

    private val authorId get() = params.authorId

    // Assuming the records are ready when the records page is opened, and resolve the
    // categories right away to ease the database observation handling.
    private val categories = recordsObserver.records.value
        .orEmpty()
        .filter { it.type == params.type && (authorId == null || it.author?.id == authorId) }
        .groupBy { it.category }
        .toList()
        .sortedByDescending { (_, v) -> v.sumOf { it.price } }
        .map { it.first }
        // When the app was previously killed, the RecordsObserver will be cleared, added
        // the target category from screen param manually to avoid displaying empty data.
        .ifEmpty { listOf(params.category) }

    val initialPage = categories.indexOf(params.category).coerceAtLeast(0)
    val pageSize get() = categories.size

    private val pageIndex = MutableStateFlow(initialPage)

    val category = pageIndex.mapState {
        categories.getOrNull(it) ?: params.category
    }

    val recordsList = combine(
        recordsObserver.records,
        sortMode
    ) { allRecords, sortMode ->
        allRecords ?: return@combine null
        categories.map { category ->
            allRecords
                .filter { record ->
                    record.category == category &&
                        record.type == params.type &&
                        (authorId == null || record.author?.id == authorId)
                }
                .map(userRepo::resolveAuthor)
                .run {
                    when (sortMode) {
                        RecordsSortMode.Date -> sortedByDescending { it.createdOn }
                        RecordsSortMode.Price -> sortedByDescending { it.price }
                    }
                }
                .toList()
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val totalPrice = recordsList.combineState(pageIndex, viewModelScope) { recordsList, pageIndex ->
        val total = recordsList?.getOrNull(pageIndex).orEmpty().sumOf { it.price }
        bookRepo.formatPrice(total, alwaysShowSymbol = true)
    }

    fun setSortMode(newSortMode: RecordsSortMode) {
        viewModelScope.launch {
            preference.update(sortModeKey, RecordsSortMode.serializer(), newSortMode)
        }
        tracker.logEvent("overview_sort_mode_changed")
    }

    fun highlightSortingButton(dest: BubbleDest) {
        viewModelScope.launch { bubbleRepo.addBubbleToQueue(dest) }
    }

    fun canEditRecord(record: Record): Boolean {
        return bookRepo.canEdit || record.author?.id == authManager.userId
    }

    fun setPageIndex(index: Int) {
        if (pageIndex.value != index) {
            pageIndex.value = index
            tracker.logEvent("overview_records_swiped")
        }
    }

    fun duplicateRecord(record: Record) {
        recordRepo.duplicateRecord(record)
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(ViewModelScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(params: BookDest.Records): RecordsViewModel
    }
}