package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.SelectableDates
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
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.DatePickerDialog as MaterialDatePickerDialog

@Composable
actual fun DatePickerDialog(
    date: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?,
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
