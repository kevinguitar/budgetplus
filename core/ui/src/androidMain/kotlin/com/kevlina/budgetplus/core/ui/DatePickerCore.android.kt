package com.kevlina.budgetplus.core.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
internal actual fun DatePickerCore(
    initialSelectedDateMillis: Long?,
    selectableDates: SelectableDates,
    colors: DatePickerColors,
    modifier: Modifier,
    onSelectedDateChange: (Long?) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis,
        selectableDates = selectableDates,
    )

    LaunchedEffect(state.selectedDateMillis) {
        onSelectedDateChange(state.selectedDateMillis)
    }

    DatePicker(
        state = state,
        colors = colors,
        showModeToggle = true,
        modifier = modifier,
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
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialSelectedStartDateMillis,
        initialSelectedEndDateMillis = initialSelectedEndDateMillis,
        selectableDates = selectableDates,
    )

    LaunchedEffect(state.selectedStartDateMillis, state.selectedEndDateMillis) {
        onSelectedRangeChange(state.selectedStartDateMillis, state.selectedEndDateMillis)
    }

    val dateFormatter = remember { DatePickerDefaults.dateFormatter() }
    DateRangePicker(
        state = state,
        colors = colors,
        showModeToggle = true,
        modifier = modifier,
        dateFormatter = dateFormatter,
        title = {
            DateRangePickerDefaults.DateRangePickerTitle(
                displayMode = state.displayMode,
                contentColor = colors.titleContentColor
            )
        },
        headline = {
            DateRangePickerDefaults.DateRangePickerHeadline(
                selectedStartDateMillis = state.selectedStartDateMillis,
                selectedEndDateMillis = state.selectedEndDateMillis,
                displayMode = state.displayMode,
                dateFormatter = dateFormatter,
                contentColor = colors.headlineContentColor,
            )
        },
    )
}

