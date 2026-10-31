package com.kevlina.budgetplus.feature.record.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.app_name
import budgetplus.core.common.generated.resources.record_widget_limit
import budgetplus.core.common.generated.resources.record_widget_pin_unsupported
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.Logger
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.time.Clock

@ViewModelKey
@ContributesIntoMap(AppScope::class)
internal class RecordWidgetViewModel(
    bookRepo: BookRepo,
    val navController: NavController<BookDest>,
    private val authManager: AuthManager,
    private val frequentRecordsClient: FrequentRecordsClient,
    private val configStore: RecordWidgetConfigStore,
    private val widgetPlatform: RecordWidgetPlatform,
    private val snackbarSender: SnackbarSender,
    private val tracker: Tracker,
) : ViewModel() {

    /**
     * The widget is tied to the book which is current when the screen opens.
     */
    val book: Book? = bookRepo.bookState.value

    val isSignedIn: Boolean get() = authManager.userState.value != null

    val uiState: StateFlow<RecordWidgetUiState>
        field = MutableStateFlow<RecordWidgetUiState>(RecordWidgetUiState.Loading)

    val selectedItems: StateFlow<List<RecordWidgetItem>>
        field = MutableStateFlow<List<RecordWidgetItem>>(emptyList())

    val isDoneEnabled: StateFlow<Boolean> = selectedItems
        .map { it.size in RECORD_WIDGET_MIN_ITEMS..RECORD_WIDGET_MAX_ITEMS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val showPresetSavedEvent: EventFlow<Unit>
        field = MutableEventFlow<Unit>()

    init {
        loadFrequentItems()
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

    /**
     * Build the config of the current selection, null if the selection is invalid.
     */
    fun buildConfig(id: String = randomUUID()): RecordWidgetConfig? {
        val book = book ?: return null
        val items = selectedItems.value
        if (items.size !in RECORD_WIDGET_MIN_ITEMS..RECORD_WIDGET_MAX_ITEMS) return null

        return RecordWidgetConfig(
            id = id,
            bookId = book.id,
            bookName = book.name,
            items = items,
            createdOn = Clock.System.now().toEpochMilliseconds(),
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
     * Save the config of a widget which was added from the OS widget picker.
     */
    suspend fun saveConfigForWidget(widgetId: String): RecordWidgetConfig? {
        val config = buildConfig(id = widgetId) ?: return null
        configStore.save(config)
        widgetPlatform.refreshWidgets()
        tracker.trackRecordWidgetAdded(source = RecordWidgetAddedSource.Os, itemCount = config.items.size)
        return config
    }

    fun trackPresetSaved() {
        tracker.logEvent("record_widget_preset_saved")
    }

    private fun loadFrequentItems() {
        val bookId = book?.id
        if (bookId == null) {
            uiState.value = RecordWidgetUiState.Loaded(emptyList())
            return
        }

        viewModelScope.launch {
            uiState.value = try {
                val fromDate = LocalDate.now().minus(FREQUENT_RECORDS_DAYS, DateTimeUnit.DAY)
                val records = frequentRecordsClient.queryRecords(bookId = bookId, fromDate = fromDate)
                RecordWidgetUiState.Loaded(records.toFrequentItems())
            } catch (e: Exception) {
                Logger.e(e, "Fail to load the frequent records")
                snackbarSender.sendError(e)
                RecordWidgetUiState.Loaded(emptyList())
            }
        }
    }
}

sealed interface RecordWidgetUiState {
    data object Loading : RecordWidgetUiState
    data class Loaded(val items: List<FrequentItem>) : RecordWidgetUiState
}
