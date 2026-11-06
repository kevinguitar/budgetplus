package com.kevlina.budgetplus.feature.record.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.app_name
import budgetplus.core.common.generated.resources.record_widget_book_unavailable
import budgetplus.core.common.generated.resources.record_widget_limit
import budgetplus.core.common.generated.resources.record_widget_pin_unsupported
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.Logger
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.common.randomUUID
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.remote.Book
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@ViewModelKey
@ContributesIntoMap(AppScope::class)
internal class RecordWidgetViewModel(
    private val bookRepo: BookRepo,
    val navController: NavController<BookDest>,
    private val authManager: AuthManager,
    private val frequentRecordsClient: FrequentRecordsClient,
    private val configStore: RecordWidgetConfigStore,
    private val widgetPlatform: RecordWidgetPlatform,
    private val snackbarSender: SnackbarSender,
    private val tracker: Tracker,
) : ViewModel() {

    /**
     * The book the widget is tied to. A new widget uses the book which is current when the screen
     * opens, an existing widget keeps its own book.
     */
    val book: StateFlow<Book?>
        field = MutableStateFlow(bookRepo.bookState.value)

    /**
     * The books the widget can be tied to, archived books are already excluded.
     */
    val books: StateFlow<List<Book>> = bookRepo.booksState.mapState { it.orEmpty() }

    val isSignedIn: Boolean get() = authManager.userState.value != null

    val uiState: StateFlow<RecordWidgetUiState>
        field = MutableStateFlow<RecordWidgetUiState>(RecordWidgetUiState.Loading)

    /**
     * The selected items, in the order they appear on the widget.
     */
    val selectedItems: StateFlow<List<RecordWidgetItem>>
        field = MutableStateFlow<List<RecordWidgetItem>>(emptyList())

    /**
     * Android 12+: color the widget by Material You instead of the app's theme.
     */
    val useDynamicColors: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val isDynamicColorSupported: Boolean get() = isRecordWidgetDynamicColorSupported

    val isDoneEnabled: StateFlow<Boolean> = selectedItems
        .map { it.size in RECORD_WIDGET_MIN_ITEMS..RECORD_WIDGET_MAX_ITEMS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val showPresetSavedEvent: EventFlow<Unit>
        field = MutableEventFlow<Unit>()

    private var isStarted = false
    private var loadJob: Job? = null

    /**
     * The config of the widget being edited, null when it's a new widget or its book is gone.
     */
    private var originalConfig: RecordWidgetConfig? = null

    /**
     * Start loading the screen, only the first call takes effect so it's safe to call again after
     * configuration changes.
     *
     * @param widgetId The Android appWidgetId when the screen is opened from the launcher, null
     *  when it's opened from the app's settings. When the widget is already configured, i.e. it's
     *  being edited, its items are preselected in the same order.
     */
    fun start(widgetId: String?) {
        if (isStarted) return
        isStarted = true

        loadJob = viewModelScope.launch {
            val existingConfig = widgetId?.let { configStore.get(it) }
            val bookId = if (existingConfig != null) {
                resolveExistingConfig(existingConfig)
            } else {
                book.value?.id
            }
            originalConfig = existingConfig?.takeIf { it.bookId == bookId }
            loadFrequentItems(bookId = bookId, pinnedItems = originalConfig?.items.orEmpty())
        }
    }

    /**
     * Tie the widget to another book. It doesn't change the app's current book.
     *
     * The items are book specific, so the selection is cleared. When editing a widget and switching
     * back to its original book, the original selection is restored.
     */
    fun selectBook(book: Book) {
        if (book.id == this.book.value?.id) return

        this.book.value = book
        val pinnedItems = originalConfig?.takeIf { it.bookId == book.id }?.items.orEmpty()
        selectedItems.value = pinnedItems
        tracker.logEvent("record_widget_book_changed")

        // Discard the pending load of the previous book, so it can't overwrite the list.
        loadJob?.cancel()
        uiState.value = RecordWidgetUiState.Loading
        loadJob = viewModelScope.launch {
            loadFrequentItems(bookId = book.id, pinnedItems = pinnedItems)
        }
    }

    fun toggleItem(item: RecordWidgetItem) {
        val selected = selectedItems.value
        if (item in selected) {
            selectedItems.update { it - item }
            return
        }

        if (selected.size >= RECORD_WIDGET_MAX_ITEMS) {
            viewModelScope.launch {
                snackbarSender.send(Res.string.record_widget_limit, formatArgs = listOf(RECORD_WIDGET_MAX_ITEMS))
            }
            return
        }
        selectedItems.update { it + item }
    }

    fun setUseDynamicColors(enabled: Boolean) {
        useDynamicColors.value = enabled
    }

    /**
     * Build the config of the current selection, null if the selection is invalid.
     */
    fun buildConfig(id: String = randomUUID()): RecordWidgetConfig? {
        val book = book.value ?: return null
        val items = selectedItems.value
        if (items.size !in RECORD_WIDGET_MIN_ITEMS..RECORD_WIDGET_MAX_ITEMS) return null

        return RecordWidgetConfig(
            id = id,
            bookId = book.id,
            bookName = book.name,
            items = items,
            createdOn = Clock.System.now().toEpochMilliseconds(),
            useDynamicColors = useDynamicColors.value,
        )
    }

    /**
     * Android: request the launcher to pin the widget. iOS: save the config as a preset.
     */
    fun addWidgetFromSettings() {
        val config = buildConfig() ?: return
        viewModelScope.launch {
            try {
                when (widgetPlatform.addWidget(config)) {
                    AddWidgetResult.PinRequested -> navController.navigateUp()
                    AddWidgetResult.PinUnsupported -> {
                        snackbarSender.send(
                            Res.string.record_widget_pin_unsupported,
                            formatArgs = listOf(Res.string.app_name),
                        )
                    }

                    AddWidgetResult.PresetSaved -> showPresetSavedEvent.sendEvent()
                }
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    /**
     * Save the config of a widget which was added from the OS widget picker, or reconfigured.
     */
    suspend fun saveConfigForWidget(widgetId: String): RecordWidgetConfig? {
        val newConfig = buildConfig(id = widgetId) ?: return null
        val existingConfig = configStore.get(widgetId)
        // Keep when the widget was created, editing doesn't make it a new widget.
        val config = if (existingConfig != null) {
            newConfig.copy(createdOn = existingConfig.createdOn)
        } else {
            newConfig
        }
        configStore.save(config)
        widgetPlatform.refreshWidgets()
        if (existingConfig == null) {
            tracker.trackRecordWidgetAdded(
                source = RecordWidgetAddedSource.Os,
                itemCount = config.items.size,
                useDynamicColors = config.useDynamicColors,
            )
        } else {
            tracker.trackRecordWidgetEdited(
                itemCount = config.items.size,
                isBookChanged = existingConfig.bookId != config.bookId,
                useDynamicColors = config.useDynamicColors,
            )
        }
        return config
    }

    fun trackPresetSaved() {
        tracker.logEvent("record_widget_preset_saved")
    }

    /**
     * Use the book of the widget being edited and preselect its items.
     *
     * @return The id of the book to load the frequent items from.
     */
    private suspend fun resolveExistingConfig(config: RecordWidgetConfig): String? {
        // The colors are a widget level setting, they're kept even when the book is gone.
        useDynamicColors.value = config.useDynamicColors

        // Show the snapshot right away, then verify the book is still available.
        book.value = Book(id = config.bookId, name = config.bookName)
        selectedItems.value = config.items

        val books = withTimeoutOrNull(DATA_LOADING_TIMEOUT) {
            bookRepo.booksState.filterNotNull().first()
        }
        if (books == null) {
            // The books aren't loaded in time, it doesn't mean the book is gone. Keep the snapshot.
            Logger.w("Record widget: books aren't loaded in time")
            return config.bookId
        }

        val widgetBook = books.firstOrNull { it.id == config.bookId }
        if (widgetBook != null) {
            book.value = widgetBook
            return widgetBook.id
        }

        // The book was deleted or the user left it, start over with the current book. Pick it from
        // the loaded books, the repo updates the current book after the books, so it could still be
        // the unavailable book at this point. Fall back to the last book like the repo does.
        book.value = books.firstOrNull { it.id == bookRepo.currentBookId } ?: books.lastOrNull()
        selectedItems.value = emptyList()
        snackbarSender.send(Res.string.record_widget_book_unavailable)
        return book.value?.id
    }

    /**
     * @param pinnedItems The items of the widget being edited. They're listed first in the widget
     *  order, even when they're no longer frequently recorded, so the user can still see and
     *  unselect them.
     */
    private suspend fun loadFrequentItems(bookId: String?, pinnedItems: List<RecordWidgetItem>) {
        if (bookId == null) {
            uiState.value = RecordWidgetUiState.Loaded(emptyList())
            return
        }

        uiState.value = try {
            val fromDate = LocalDate.now().minus(FREQUENT_RECORDS_DAYS, DateTimeUnit.DAY)
            val records = frequentRecordsClient.queryRecords(bookId = bookId, fromDate = fromDate)
            RecordWidgetUiState.Loaded(records.toFrequentItems(pinnedItems = pinnedItems))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e, "Fail to load the frequent records")
            snackbarSender.sendError(e)
            RecordWidgetUiState.Loaded(pinnedItems.map { FrequentItem(item = it, count = 0) })
        }
    }
}

private val DATA_LOADING_TIMEOUT = 5.seconds

sealed interface RecordWidgetUiState {
    data object Loading : RecordWidgetUiState
    data class Loaded(val items: List<FrequentItem>) : RecordWidgetUiState
}
