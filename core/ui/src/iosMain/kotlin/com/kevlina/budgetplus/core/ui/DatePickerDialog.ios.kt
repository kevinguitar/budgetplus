package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.mohamedrejeb.calf.ui.datepicker.AdaptiveDatePicker
import com.mohamedrejeb.calf.ui.datepicker.rememberAdaptiveDatePickerState
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun DatePickerDialog(
    date: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    onDatePicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberAdaptiveDatePickerState(
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

    AppDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .sizeIn(maxWidth = 480.dp, maxHeight = 560.dp)
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AdaptiveDatePicker(
                state = state,
                colors = datePickerColors(),
                modifier = Modifier.fillMaxWidth()
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
                            onDatePicked(state.selectedDateMillis?.utcLocaleDate ?: date)
                            onDismiss()
                        }
                        .padding(all = 16.dp)
                )
            }
        }
    }
}
