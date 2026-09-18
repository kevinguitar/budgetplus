package com.kevlina.budgetplus.core.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate

/**
 * Adaptive date range picker dialog.
 *
 * On Android it uses the themed Material3 [androidx.compose.material3.DateRangePicker]
 * (unchanged), while on iOS it renders Calf's native `AdaptiveDateRangePicker`
 * (a multi-date `UICalendarView`) for a native look & feel.
 */
@Composable
expect fun DateRangePickerDialog(
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    onRangePicked: (from: LocalDate, until: LocalDate) -> Unit,
    onDismiss: () -> Unit,
)
