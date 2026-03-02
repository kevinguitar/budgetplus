package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.overview_period_day
import budgetplus.core.common.generated.resources.overview_period_last_month
import budgetplus.core.common.generated.resources.overview_period_month
import budgetplus.core.common.generated.resources.overview_period_week
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.remote.TimePeriod
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.DateRangePickerDialog
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.rippleClick
import com.kevlina.budgetplus.feature.overview.OverviewTimeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ColumnScope.TimePeriodSelector(
    state: TimePeriodSelectorState,
    navController: NavController<BookDest>,
) {
    val timePeriod by state.timePeriod.collectAsStateWithLifecycle()
    val fromDate by state.fromDate.collectAsStateWithLifecycle()
    val untilDate by state.untilDate.collectAsStateWithLifecycle()

    var showDateRangerPicker by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = state.openPremiumEvent) {
        state.openPremiumEvent
            .consumeEach { navController.navigate(BookDest.UnlockPremium) }
            .collect()
    }

    DateRange(
        state = state,
        showDateRangePicker = { showDateRangerPicker = true },
    )

    TimePeriodPreset(
        timePeriod = timePeriod,
        setTimePeriod = state.setTimePeriod
    )

    if (showDateRangerPicker) {
        DateRangePickerDialog(
            startDate = fromDate,
            endDate = untilDate,
            onDismiss = { showDateRangerPicker = false },
            onRangePicked = state.setDateRange
        )
    }
}

@Composable
fun TimePeriodPreset(
    timePeriod: TimePeriod,
    setTimePeriod: (TimePeriod) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
    ) {
        setOf(
            TimePeriod.Today,
            TimePeriod.Week,
            TimePeriod.Month,
            TimePeriod.LastMonth
        )
            .forEach { period ->
                TimePeriodPill(
                    timePeriod = period,
                    isSelected = timePeriod == period,
                    onClick = { setTimePeriod(period) }
                )
            }
    }
}

@Composable
private fun TimePeriodPill(
    timePeriod: TimePeriod,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                shape = AppTheme.cardShape,
                color = if (isSelected) {
                    LocalAppColors.current.dark
                } else {
                    LocalAppColors.current.primary
                }
            )
            .clip(AppTheme.cardShape)
            .rippleClick(onClick = onClick)
    ) {

        val titleRes = when (timePeriod) {
            is TimePeriod.Today -> Res.string.overview_period_day
            is TimePeriod.Week -> Res.string.overview_period_week
            is TimePeriod.Month -> Res.string.overview_period_month
            is TimePeriod.LastMonth -> Res.string.overview_period_last_month
            is TimePeriod.Custom -> error("Custom period doesn't shown in pill.")
        }

        Text(
            text = stringResource(titleRes),
            color = LocalAppColors.current.light,
            singleLine = true,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Stable
internal class TimePeriodSelectorState(
    val timePeriod: StateFlow<TimePeriod>,
    val fromDate: StateFlow<LocalDate>,
    val untilDate: StateFlow<LocalDate>,
    val isOneDayPeriod: StateFlow<Boolean>,
    val openPremiumEvent: EventFlow<Unit>,
    val previousDay: () -> Unit,
    val nextDay: () -> Unit,
    val setTimePeriod: (TimePeriod) -> Unit,
    val setDateRange: (from: LocalDate, until: LocalDate) -> Unit,
) {
    companion object {
        val preview = TimePeriodSelectorState(
            timePeriod = MutableStateFlow(TimePeriod.Month),
            fromDate = MutableStateFlow(TimePeriod.Month.from),
            untilDate = MutableStateFlow(TimePeriod.Month.until),
            isOneDayPeriod = MutableStateFlow(false),
            openPremiumEvent = MutableEventFlow(),
            previousDay = {},
            nextDay = {},
            setTimePeriod = {},
            setDateRange = { _, _ -> }
        )
    }
}

internal fun OverviewTimeViewModel.toState() = TimePeriodSelectorState(
    timePeriod = timePeriod,
    fromDate = fromDate,
    untilDate = untilDate,
    isOneDayPeriod = isOneDayPeriod,
    openPremiumEvent = openPremiumEvent,
    previousDay = ::previousDay,
    nextDay = ::nextDay,
    setTimePeriod = ::setTimePeriod,
    setDateRange = ::setDateRange,
)

@Preview
@Composable
private fun TimePeriodSelector_Preview() = AppTheme {
    Column(
        modifier = Modifier.background(LocalAppColors.current.lightBg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TimePeriodSelector(
            state = TimePeriodSelectorState.preview,
            navController = NavController.preview
        )
    }
}