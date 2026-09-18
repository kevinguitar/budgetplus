package com.kevlina.budgetplus.core.ui

import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import com.kevlina.budgetplus.core.theme.LocalAppColors
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Adaptive single-date picker dialog.
 *
 * On Android it uses the themed Material3 [androidx.compose.material3.DatePicker]
 * (unchanged), while on iOS it renders Calf's native `AdaptiveDatePicker`
 * (a `UICalendarView`/`UIDatePicker`) for a native look & feel.
 */
@Composable
expect fun DatePickerDialog(
    date: LocalDate,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    onDatePicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
)

@Composable
internal fun datePickerColors(): DatePickerColors {
    val lightColor = LocalAppColors.current.light
    val primaryColor = LocalAppColors.current.primary
    val darkColor = LocalAppColors.current.dark
    return DatePickerDefaults.colors(
        containerColor = lightColor,
        titleContentColor = darkColor,
        headlineContentColor = darkColor,
        weekdayContentColor = darkColor,
        subheadContentColor = darkColor,
        navigationContentColor = darkColor,
        yearContentColor = darkColor,
        disabledYearContentColor = darkColor.copy(alpha = DISABLED_ALPHA),
        currentYearContentColor = darkColor,
        selectedYearContentColor = lightColor,
        disabledSelectedYearContentColor = primaryColor.copy(alpha = DISABLED_ALPHA),
        selectedYearContainerColor = primaryColor,
        disabledSelectedYearContainerColor = darkColor.copy(alpha = DISABLED_ALPHA),
        dayContentColor = darkColor,
        disabledDayContentColor = darkColor.copy(alpha = DISABLED_ALPHA),
        selectedDayContentColor = lightColor,
        disabledSelectedDayContentColor = lightColor.copy(alpha = DISABLED_ALPHA),
        selectedDayContainerColor = primaryColor,
        disabledSelectedDayContainerColor = primaryColor.copy(alpha = DISABLED_ALPHA),
        todayContentColor = darkColor,
        todayDateBorderColor = primaryColor,
        dayInSelectionRangeContentColor = darkColor,
        dayInSelectionRangeContainerColor = primaryColor.copy(alpha = SELECTION_ALPHA),
        dividerColor = darkColor,
        dateTextFieldColors = TextFieldDefaults.colors(
            focusedTextColor = darkColor,
            unfocusedTextColor = primaryColor,
            focusedContainerColor = lightColor,
            unfocusedContainerColor = lightColor,
            errorContainerColor = lightColor,
            focusedIndicatorColor = darkColor,
            unfocusedIndicatorColor = primaryColor,
            focusedLabelColor = darkColor,
            unfocusedLabelColor = primaryColor,
            focusedPlaceholderColor = darkColor,
            unfocusedPlaceholderColor = primaryColor,
            focusedSupportingTextColor = darkColor,
            unfocusedSupportingTextColor = darkColor,
            focusedPrefixColor = darkColor,
            unfocusedPrefixColor = primaryColor,
            focusedSuffixColor = darkColor,
            unfocusedSuffixColor = primaryColor,
            cursorColor = darkColor
        )
    )
}

private const val DISABLED_ALPHA = 0.38f
private const val SELECTION_ALPHA = 0.2f

internal val LocalDate.utcMillis: Long
    get() = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

internal val Long.utcLocaleDate: LocalDate
    get() = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
