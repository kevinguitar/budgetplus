package com.kevlina.budgetplus.feature.overview

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.cta_go
import budgetplus.core.common.generated.resources.overview_exceed_max_period
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.RecordsObserver
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.data.remote.TimePeriod
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@Inject
class OverviewTimeViewModel(
    private val recordsObserver: RecordsObserver,
    private val bookRepo: BookRepo,
    private val authManager: AuthManager,
    private val tracker: Tracker,
    private val snackbarSender: SnackbarSender,
    private val preference: Preference,
) : ViewModel() {

    val timePeriod = recordsObserver.timePeriod
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), TimePeriod.Month)

    val fromDate = timePeriod.mapState { it.from }
    val untilDate = timePeriod.mapState { it.until }
    val isOneDayPeriod = timePeriod.mapState { it.from == it.until }

    private fun buildCustomPeriodKey(bookId: String) =
        stringPreferencesKey("custom_period_for_$bookId")

    val customPeriod = bookRepo.bookState
        .flatMapLatest { book ->
            val bookId = book?.id ?: return@flatMapLatest emptyFlow()
            preference.of(
                key = buildCustomPeriodKey(bookId),
                serializer = TimePeriod.serializer()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val openPremiumEvent: EventFlow<Unit>
        field = MutableEventFlow<Unit>()

    fun setTimePeriod(timePeriod: TimePeriod, isCustomized: Boolean = false) {
        val bookId = bookRepo.currentBookId ?: return
        val isAboveOneMonth = timePeriod.from < timePeriod.until.minus(1, DateTimeUnit.MONTH)
        val period = if (!authManager.isPremium.value && isAboveOneMonth) {
            tracker.logEvent("overview_exceed_max_period")
            viewModelScope.launch {
                snackbarSender.send(
                    message = Res.string.overview_exceed_max_period,
                    actionLabel = Res.string.cta_go,
                    action = {
                        tracker.logEvent("overview_exceed_max_period_unlock")
                        openPremiumEvent.sendEvent()
                    }
                )
            }

            TimePeriod.Custom(
                from = timePeriod.from,
                until = timePeriod.from.plus(1, DateTimeUnit.MONTH)
            )
        } else {
            timePeriod
        }

        recordsObserver.setTimePeriod(bookId, period)

        if (isCustomized) {
            viewModelScope.launch {
                preference.update(
                    key = buildCustomPeriodKey(bookId),
                    serializer = TimePeriod.serializer(),
                    value = timePeriod
                )
            }
        }
    }

    fun setDateRange(from: LocalDate, until: LocalDate, isCustomized: Boolean = false) {
        setTimePeriod(TimePeriod.Custom(from = from, until = until), isCustomized)
    }

    fun previousDay() {
        val previousDay = fromDate.value.minus(1, DateTimeUnit.DAY)
        setDateRange(from = previousDay, until = previousDay)
    }

    fun nextDay() {
        val nextDay = fromDate.value.plus(1, DateTimeUnit.DAY)
        setDateRange(from = nextDay, until = nextDay)
    }
}