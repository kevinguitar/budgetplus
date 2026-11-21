package com.kevlina.budgetplus.feature.overview

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.cta_open_settings
import budgetplus.core.common.generated.resources.write_storage_permission_hint
import com.kevlina.budgetplus.core.ads.InterstitialAdsHandler
import com.kevlina.budgetplus.core.ads.InterstitialPlacement
import com.kevlina.budgetplus.core.common.OpenAppSettingsAction
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.common.mediumFormatted
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.CurrencyExchangeRepo
import com.kevlina.budgetplus.core.data.RecordRepo
import com.kevlina.budgetplus.core.data.RecordsObserver
import com.kevlina.budgetplus.core.data.UserRepo
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.data.remote.createdOn
import com.kevlina.budgetplus.core.data.resolveAuthor
import com.kevlina.budgetplus.core.settings.api.ChartModeSettings
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.bubble.BubbleRepo
import com.kevlina.budgetplus.feature.overview.ui.CurrencyToggleState
import com.kevlina.budgetplus.feature.overview.ui.OverviewContentState
import com.kevlina.budgetplus.feature.overview.ui.OverviewHeaderState
import com.kevlina.budgetplus.feature.overview.ui.OverviewListState
import com.kevlina.budgetplus.feature.overview.ui.toState
import com.kevlina.budgetplus.feature.overview.utils.CsvExporter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@ViewModelKey
@ContributesIntoMap(AppScope::class)
internal class OverviewViewModel(
    private val recordRepo: RecordRepo,
    private val recordsObserver: RecordsObserver,
    private val tracker: Tracker,
    private val authManager: AuthManager,
    private val userRepo: UserRepo,
    private val bubbleRepo: BubbleRepo,
    private val csvExporter: CsvExporter,
    private val snackbarSender: SnackbarSender,
    private val interstitialAdsHandler: InterstitialAdsHandler,
    val navController: NavController<BookDest>,
    private val bookRepo: BookRepo,
    val timeModel: OverviewTimeViewModel,
    val chartModeSettings: ChartModeSettings,
    private val preference: Preference,
    private val currencyExchangeRepo: CurrencyExchangeRepo,
    private val openAppSettingsAction: OpenAppSettingsAction,
) : ViewModel() {

    val bookName = bookRepo.bookState.mapState { it?.name }
    private val isSoloAuthor = bookRepo.bookState.mapState { it?.authors?.size == 1 }

    private val typeKey = stringPreferencesKey("typeCache")
    private val type = preference.of(
        typeKey, RecordType.serializer(), default = RecordType.Expense, scope = viewModelScope
    )

    private val modeKey = stringPreferencesKey("modeCache")
    internal val mode: StateFlow<OverviewMode> = preference.of(
        modeKey, OverviewMode.serializer(), default = OverviewMode.AllRecords, scope = viewModelScope
    )

    private var modeBubbleJob: Job? = null
    private var exportBubbleJob: Job? = null
    private var tapHintBubbleJob: Job? = null
    private var pieChartBubbleJob: Job? = null

    private val authors = bookRepo.bookState
        .map {
            withContext(Dispatchers.Default) {
                it?.authors
                    .orEmpty()
                    .mapNotNull(userRepo::getUser)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    private val selectedAuthor = MutableStateFlow<User?>(null)

    private val records: Flow<Sequence<Record>?> = combine(
        recordsObserver.records,
        type,
        selectedAuthor
    ) { records, type, author ->
        val authorId = author?.id
        records?.filter {
            it.type == type && (authorId == null || it.author?.id == authorId)
        }
    }

    private val totalPrice = combine(
        records,
        currencyExchangeRepo.displayInPreferredCurrency
    ) { records, _ ->
        records.orEmpty().sumOf(currencyExchangeRepo::getDisplayPrice)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    private val totalFormattedPrice = combine(
        totalPrice,
        currencyExchangeRepo.displayInPreferredCurrency
    ) { price, _ ->
        currencyExchangeRepo.formatDisplayPrice(price, alwaysShowSymbol = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    private val formattedBalance = combine(
        recordsObserver.records.filterNotNull(),
        selectedAuthor,
        currencyExchangeRepo.displayInPreferredCurrency
    ) { records, author, _ ->
        val authorId = author?.id
        val sum = withContext(Dispatchers.Default) {
            records
                .filter { authorId == null || it.author?.id == authorId }
                .sumOf { record ->
                    val displayPrice = currencyExchangeRepo.getDisplayPrice(record)
                    when (record.type) {
                        RecordType.Expense -> -displayPrice
                        RecordType.Income -> displayPrice
                    }
                }
        }
        currencyExchangeRepo.formatDisplayPrice(sum, alwaysShowSymbol = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    private val recordList: StateFlow<List<Record>?> = records.map { records ->
        records
            ?.map(userRepo::resolveAuthor)
            ?.sortedByDescending { it.createdOn }
            ?.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val recordGroups: StateFlow<Map<String, List<Record>>?> = records.map { records ->
        records ?: return@map null
        withContext(Dispatchers.Default) {
            records
                .groupBy { it.category }
                .toList()
                .sortedByDescending { (_, v) -> v.sumOf { it.price } }
                .toMap()
                .mapValues { it.value }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val currencyToggleState: StateFlow<CurrencyToggleState?> = combine(
        bookRepo.currencySymbol,
        currencyExchangeRepo.preferredCurrencySymbol,
        currencyExchangeRepo.displayInPreferredCurrency
    ) { bookCurrencySymbol, preferredCurrencySymbol, currencyToggle ->
        if (preferredCurrencySymbol == null || bookCurrencySymbol == preferredCurrencySymbol) {
            return@combine null
        }

        CurrencyToggleState(
            bookCurrencySymbol = bookCurrencySymbol,
            preferredCurrencySymbol = preferredCurrencySymbol,
            toggleState = currencyToggle,
            onClick = ::onCurrencyToggled
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    internal val state = OverviewContentState(
        headerState = OverviewHeaderState(
            type = type,
            totalPrice = totalFormattedPrice,
            balance = formattedBalance,
            recordGroups = recordGroups,
            currencyToggleState = currencyToggleState,
            authors = authors,
            selectedAuthor = selectedAuthor,
            timePeriodSelectorState = timeModel.toState(),
            setRecordType = ::setRecordType,
            setAuthor = ::setAuthor
        ),
        listState = OverviewListState(
            mode = mode,
            chartMode = chartModeSettings.chartMode,
            type = type,
            selectedAuthor = selectedAuthor,
            totalPrice = totalPrice,
            recordList = recordList,
            recordGroups = recordGroups,
            isSoloAuthor = isSoloAuthor,
            currencyToggleState = currencyToggleState.mapState { it?.toggleState ?: false },
            highlightTapHint = ::highlightTapHint,
            highlightPieChart = ::highlightPieChart,
            getDisplayPrice = currencyExchangeRepo::getDisplayPrice,
            formatPrice = { price -> currencyExchangeRepo.formatDisplayPrice(price) },
            canEditRecord = ::canEditRecord,
            duplicateRecord = ::duplicateRecord,
            onGroupClicked = ::onGroupClicked
        )
    )

    fun toggleMode() {
        val newMode = when (mode.value) {
            OverviewMode.AllRecords -> OverviewMode.GroupByCategories
            OverviewMode.GroupByCategories -> OverviewMode.AllRecords
        }
        viewModelScope.launch {
            preference.update(modeKey, OverviewMode.serializer(), newMode)
        }
        tracker.logEvent("overview_mode_changed")
    }

    fun exportToCsv() {
        tracker.logEvent("overview_export_to_csv")
        interstitialAdsHandler.showAdThen(InterstitialPlacement.CsvExport, onComplete = ::performCsvExport)
    }

    private fun performCsvExport() {
        viewModelScope.launch {
            try {
                val period = recordsObserver.timePeriod.first()
                val name = if (period.from == period.until) {
                    "${bookName.value}_${period.from.mediumFormatted}"
                } else {
                    "${bookName.value}_${period.from.mediumFormatted}_${period.until.mediumFormatted}"
                }
                csvExporter.downloadRecordsToCsv(name)
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    suspend fun showWriteFilePermissionHint() {
        snackbarSender.send(
            message = Res.string.write_storage_permission_hint,
            actionLabel = Res.string.cta_open_settings,
            action = openAppSettingsAction
        )
    }

    private fun setRecordType(newType: RecordType) {
        viewModelScope.launch {
            preference.update(typeKey, RecordType.serializer(), newType)
        }
        tracker.logEvent("overview_type_changed")
    }

    private fun setAuthor(author: User?) {
        selectedAuthor.value = author
    }

    private fun canEditRecord(record: Record): Boolean {
        return bookRepo.canEdit || record.author?.id == authManager.userId
    }

    private fun duplicateRecord(record: Record) {
        recordRepo.duplicateRecord(record)
    }

    private fun onGroupClicked() {
        tracker.logEvent(
            event = "overview_group_clicked",
            params = mapOf("chart_mode" to chartModeSettings.chartModeAnalyticsName)
        )
    }

    fun highlightModeButton(dest: BubbleDest) {
        if (modeBubbleJob != null) return
        modeBubbleJob = viewModelScope.launch {
            if (recordList.filterNotNull().first().isNotEmpty()) {
                bubbleRepo.addBubbleToQueue(dest)
            }
        }
    }

    fun highlightExportButton(dest: BubbleDest) {
        if (exportBubbleJob != null) return
        exportBubbleJob = viewModelScope.launch {
            if (recordList.filterNotNull().first().isNotEmpty()) {
                bubbleRepo.addBubbleToQueue(dest)
            }
        }
    }

    private fun highlightTapHint(dest: BubbleDest) {
        if (tapHintBubbleJob != null) return
        tapHintBubbleJob = viewModelScope.launch {
            delay(animationDelay)
            bubbleRepo.addBubbleToQueue(dest)
        }
    }

    private fun highlightPieChart(dest: BubbleDest) {
        if (pieChartBubbleJob != null) return
        pieChartBubbleJob = viewModelScope.launch {
            delay(animationDelay)
            bubbleRepo.addBubbleToQueue(dest)
        }
    }

    private fun onCurrencyToggled() {
        if (authManager.isPremium.value) {
            currencyExchangeRepo.toggleDisplayInPreferredCurrency()
        } else {
            navController.navigate(BookDest.UnlockPremium)
        }
        tracker.logEvent("currency_exchange_toggle_overview")
    }

    companion object {
        private val animationDelay = 200.milliseconds
    }
}