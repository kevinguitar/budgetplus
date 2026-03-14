package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource

@Composable
fun DateRangePickerDialog(
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    onRangePicked: (from: LocalDate, until: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startDate?.utcMillis,
        initialSelectedEndDateMillis = endDate?.utcMillis,
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

    AppDialog(
        usePlatformDefaultWidth = false,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .sizeIn(maxWidth = 480.dp, maxHeight = 560.dp)
            .padding(vertical = 16.dp)
    ) {
        Column {
            val dateFormatter = remember { DatePickerDefaults.dateFormatter() }
            val colors = datePickerColors()
            DateRangePicker(
                state = state,
                colors = colors,
                showModeToggle = true,
                modifier = Modifier.weight(1F),
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
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                },
            )

            Row(modifier = Modifier.align(Alignment.End)) {
                Text(
                    text = stringResource(Res.string.cta_cancel),
                    color = LocalAppColors.current.dark,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(8.dp))
                        .rippleClick(onClick = onDismiss)
                        .padding(all = 16.dp)
                )

                Text(
                    text = stringResource(Res.string.cta_confirm),
                    color = LocalAppColors.current.dark,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(8.dp))
                        .rippleClick {
                            val startDate = state.selectedStartDateMillis?.utcLocaleDate
                            val endDate = state.selectedEndDateMillis?.utcLocaleDate
                            if (startDate != null && endDate != null) {
                                onRangePicked(startDate, endDate)
                            }
                            onDismiss()
                        }
                        .padding(all = 16.dp)
                )
            }
        }
    }
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