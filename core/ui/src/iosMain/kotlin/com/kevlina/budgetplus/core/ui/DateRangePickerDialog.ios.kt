package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.cta_cancel
import budgetplus.core.common.generated.resources.cta_confirm
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.mohamedrejeb.calf.ui.datepicker.AdaptiveDateRangePicker
import com.mohamedrejeb.calf.ui.datepicker.rememberAdaptiveDateRangePickerState
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun DateRangePickerDialog(
    startDate: LocalDate?,
    endDate: LocalDate?,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    onRangePicked: (from: LocalDate, until: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberAdaptiveDateRangePickerState(
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
            AdaptiveDateRangePicker(
                state = state,
                colors = datePickerColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .height(420.dp)
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

                val isRangeSelected = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null
                Text(
                    text = stringResource(Res.string.cta_confirm),
                    color = if (isRangeSelected) {
                        LocalAppColors.current.dark
                    } else {
                        LocalAppColors.current.dark.copy(alpha = 0.4f)
                    },
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(8.dp))
                        .thenIf(isRangeSelected) {
                            Modifier.rippleClick {
                                val start = state.selectedStartDateMillis?.utcLocaleDate
                                val end = state.selectedEndDateMillis?.utcLocaleDate
                                if (start != null && end != null) {
                                    onRangePicked(start, end)
                                }
                                onDismiss()
                            }
                        }
                        .padding(all = 16.dp)
                )
            }
        }
    }
}
