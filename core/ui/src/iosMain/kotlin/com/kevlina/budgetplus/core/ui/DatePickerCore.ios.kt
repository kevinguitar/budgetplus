package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.mohamedrejeb.calf.ui.datepicker.AdaptiveDatePicker
import com.mohamedrejeb.calf.ui.datepicker.AdaptiveDateRangePicker
import com.mohamedrejeb.calf.ui.datepicker.rememberAdaptiveDatePickerState
import com.mohamedrejeb.calf.ui.datepicker.rememberAdaptiveDateRangePickerState

@Composable
internal actual fun DatePickerCore(
    initialSelectedDateMillis: Long?,
    selectableDates: SelectableDates,
    colors: DatePickerColors,
    modifier: Modifier,
    onSelectedDateChange: (Long?) -> Unit,
) {
    val state = rememberAdaptiveDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis,
        selectableDates = selectableDates,
    )

    LaunchedEffect(state.selectedDateMillis) {
        onSelectedDateChange(state.selectedDateMillis)
    }

    AdaptiveDatePicker(
        state = state,
        colors = colors,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal actual fun DateRangePickerCore(
    initialSelectedStartDateMillis: Long?,
    initialSelectedEndDateMillis: Long?,
    selectableDates: SelectableDates,
    colors: DatePickerColors,
    modifier: Modifier,
    onSelectedRangeChange: (start: Long?, end: Long?) -> Unit,
) {
    val state = rememberAdaptiveDateRangePickerState(
        initialSelectedStartDateMillis = initialSelectedStartDateMillis,
        initialSelectedEndDateMillis = initialSelectedEndDateMillis,
        selectableDates = selectableDates,
    )

    LaunchedEffect(state.selectedStartDateMillis, state.selectedEndDateMillis) {
        onSelectedRangeChange(state.selectedStartDateMillis, state.selectedEndDateMillis)
    }

    AdaptiveDateRangePicker(
        state = state,
        colors = colors,
        modifier = modifier.fillMaxWidth(),
    )
}
