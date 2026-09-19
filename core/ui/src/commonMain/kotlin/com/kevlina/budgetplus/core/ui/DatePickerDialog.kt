package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

/**
 * Adaptive single-date picker dialog.
 *
 * The dialog chrome (background, Cancel/Confirm buttons) and the date-selection
 * business logic live here in commonMain; only the calendar itself is
 * platform-specific ([DatePickerCore]): Material3 `DatePicker` on Android, Calf's
 * native `AdaptiveDatePicker` on iOS.
 */
@Composable
fun DatePickerDialog(
    date: LocalDate,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    onDatePicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedMillis by remember { mutableStateOf<Long?>(date.utcMillis) }

    PickerDialogScaffold(
        onDismiss = onDismiss,
        picker = {
            DatePickerCore(
                initialSelectedDateMillis = date.utcMillis,
                selectableDates = dateBounds(minDate, maxDate),
                colors = datePickerColors(),
                modifier = Modifier,
                onSelectedDateChange = { selectedMillis = it },
            )
        },
        actions = {
            PickerDialogActions(
                onCancel = onDismiss,
                confirmEnabled = true,
                onConfirm = {
                    onDatePicked(selectedMillis?.utcLocaleDate ?: date)
                    onDismiss()
                },
            )
        },
    )
}

/**
 * Adaptive date range picker dialog. See [DatePickerDialog].
 */
@Composable
fun DateRangePickerDialog(
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    onRangePicked: (from: LocalDate, until: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedStartMillis by remember { mutableStateOf(startDate?.utcMillis) }
    var selectedEndMillis by remember { mutableStateOf(endDate?.utcMillis) }

    PickerDialogScaffold(
        onDismiss = onDismiss,
        picker = {
            DateRangePickerCore(
                initialSelectedStartDateMillis = startDate?.utcMillis,
                initialSelectedEndDateMillis = endDate?.utcMillis,
                selectableDates = dateBounds(minDate, maxDate),
                colors = datePickerColors(),
                modifier = Modifier,
                onSelectedRangeChange = { start, end ->
                    selectedStartMillis = start
                    selectedEndMillis = end
                },
            )
        },
        actions = {
            val isRangeSelected = selectedStartMillis != null && selectedEndMillis != null
            PickerDialogActions(
                onCancel = onDismiss,
                confirmEnabled = isRangeSelected,
                onConfirm = {
                    val start = selectedStartMillis?.utcLocaleDate
                    val end = selectedEndMillis?.utcLocaleDate
                    if (start != null && end != null) {
                        onRangePicked(start, end)
                    }
                    onDismiss()
                },
            )
        },
    )
}

/**
 * The core, platform-specific calendar for a single date. It hoists its
 * selection up through [onSelectedDateChange] so the shared dialog can drive the
 * confirm button.
 */
@Composable
internal expect fun DatePickerCore(
    initialSelectedDateMillis: Long?,
    selectableDates: SelectableDates,
    colors: DatePickerColors,
    modifier: Modifier,
    onSelectedDateChange: (Long?) -> Unit,
)

/**
 * The core, platform-specific calendar for a date range. See [DatePickerCore].
 */
@Composable
internal expect fun DateRangePickerCore(
    initialSelectedStartDateMillis: Long?,
    initialSelectedEndDateMillis: Long?,
    selectableDates: SelectableDates,
    colors: DatePickerColors,
    modifier: Modifier,
    onSelectedRangeChange: (start: Long?, end: Long?) -> Unit,
)

/**
 * The shared dialog chrome for the date pickers: a themed, full-width rounded
 * surface hosting the platform calendar and the shared [PickerDialogActions].
 *
 * A full-width surface (rather than a wrap-content dialog) is used because the
 * native iOS `UICalendarView` reports a wide intrinsic size that would otherwise
 * overflow a wrap-content dialog background.
 *
 * The [actions] row is always pinned at the bottom and never clipped. The
 * [picker] area fills the remaining height.
 */
@Composable
private fun PickerDialogScaffold(
    onDismiss: () -> Unit,
    picker: @Composable () -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .sizeIn(maxWidth = 480.dp, maxHeight = 560.dp)
                .clip(AppTheme.dialogShape)
                .background(LocalAppColors.current.light)
                .padding(16.dp),
        ) {
            Box(modifier = Modifier.weight(weight = 1f, fill = false)) {
                picker()
            }
            actions()
        }
    }
}

/**
 * The shared Cancel / Confirm button row for the date pickers.
 */
@Composable
private fun ColumnScope.PickerDialogActions(
    onCancel: () -> Unit,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
) {
    Row(modifier = Modifier.align(Alignment.End)) {
        Text(
            text = stringResource(Res.string.cta_cancel),
            color = LocalAppColors.current.dark,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(shape = RoundedCornerShape(8.dp))
                .rippleClick(onClick = onCancel)
                .padding(all = 16.dp)
        )

        Text(
            text = stringResource(Res.string.cta_confirm),
            color = if (confirmEnabled) {
                LocalAppColors.current.dark
            } else {
                LocalAppColors.current.dark.copy(alpha = 0.4f)
            },
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(shape = RoundedCornerShape(8.dp))
                .thenIf(confirmEnabled) {
                    Modifier.rippleClick(onClick = onConfirm)
                }
                .padding(all = 16.dp)
        )
    }
}

/** Shared date-selection rule: an inclusive [minDate]..[maxDate] range (per UTC day). */
internal fun dateBounds(minDate: LocalDate?, maxDate: LocalDate?): SelectableDates =
    object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean = when {
            minDate != null && maxDate != null ->
                utcTimeMillis >= minDate.utcMillis && utcTimeMillis <= maxDate.utcMillis

            minDate != null -> utcTimeMillis >= minDate.utcMillis
            maxDate != null -> utcTimeMillis <= maxDate.utcMillis
            else -> true
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

@Preview(widthDp = 600, heightDp = 600)
@Composable
private fun DateRangePickerDialog_Preview() = AppTheme(ThemeColors.Barbie) {
    DateRangePickerDialog(
        minDate = LocalDate.now().minus(1, DateTimeUnit.WEEK),
        maxDate = LocalDate.now().plus(1, DateTimeUnit.WEEK),
        onDismiss = {},
        onRangePicked = { _, _ -> },
    )
}
