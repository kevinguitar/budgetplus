package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.cta_cancel
import budgetplus.core.common.generated.resources.cta_confirm
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant
import androidx.compose.material3.DatePickerDialog as MaterialDatePickerDialog

@Composable
fun DatePickerDialog(
    date: LocalDate,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    onDatePicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = date.utcMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return when {
                    minDate != null && maxDate != null -> {
                        utcTimeMillis >= minDate.utcMillis && utcTimeMillis <= maxDate.utcMillis
                    }

                    minDate != null -> utcTimeMillis >= minDate.utcMillis
                    maxDate != null -> utcTimeMillis <= maxDate.utcMillis
                    else -> true
                }
            }
        }
    )

    val colors = datePickerColors()

    MaterialDatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        dismissButton = {
            Text(
                text = stringResource(Res.string.cta_cancel),
                color = LocalAppColors.current.dark,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(8.dp))
                    .rippleClick(onClick = onDismiss)
                    .padding(all = 16.dp)
            )
        },
        confirmButton = {
            Text(
                text = stringResource(Res.string.cta_confirm),
                color = LocalAppColors.current.dark,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clip(shape = RoundedCornerShape(8.dp))
                    .rippleClick {
                        onDatePicked(dateState.selectedDateMillis?.utcLocaleDate ?: date)
                        onDismiss()
                    }
                    .padding(all = 16.dp)
            )
        }
    ) {
        DatePicker(
            state = dateState,
            colors = colors,
            showModeToggle = true
        )
    }
}

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

@Preview
@Composable
private fun DatePickerDialog_Preview() = AppTheme(ThemeColors.Barbie) {
    DatePickerDialog(
        date = LocalDate.now(),
        minDate = LocalDate.now().minus(1, DateTimeUnit.WEEK),
        maxDate = LocalDate.now().plus(1, DateTimeUnit.WEEK),
        onDismiss = {},
        onDatePicked = {}
    )
}