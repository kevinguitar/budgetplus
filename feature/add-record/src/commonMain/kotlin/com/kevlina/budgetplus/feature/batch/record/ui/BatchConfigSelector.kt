package com.kevlina.budgetplus.feature.batch.record.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.batch_record_start_date
import budgetplus.core.common.generated.resources.batch_record_times
import budgetplus.core.common.generated.resources.ic_arrow_drop_down
import budgetplus.core.common.generated.resources.ic_numbers
import budgetplus.core.common.generated.resources.ic_today
import budgetplus.core.common.generated.resources.select_date
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.DatePickerDialog
import com.kevlina.budgetplus.core.ui.DropdownItem
import com.kevlina.budgetplus.core.ui.DropdownMenu
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.SingleDatePicker
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.rippleClick
import com.kevlina.budgetplus.core.utils.metroViewModel
import com.kevlina.budgetplus.feature.batch.record.BatchRecordViewModel
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

internal val fontSize = FontSize.SemiLarge
internal val iconModifier = Modifier
    .padding(top = 2.dp)
    .size(20.dp)

@Composable
internal fun BatchConfigSelector() {

    val vm = metroViewModel<BatchRecordViewModel>()

    val startRecordDate by vm.startRecordDate.collectAsStateWithLifecycle()
    val frequency by vm.frequency.collectAsStateWithLifecycle()
    val times by vm.times.collectAsStateWithLifecycle()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LocalAppColors.current.lightBg)
            .padding(16.dp)
    ) {

        DateSelector(date = startRecordDate.date, setStartDate = vm::setStartDate)

        FrequencySelector(
            frequency = frequency,
            setDuration = vm::setDuration,
            setUnit = vm::setUnit
        )

        TimesSelector(times = times, batchTimes = vm.batchTimes, setTimes = vm::setTimes)
    }
}

@Composable
private fun DateSelector(
    date: LocalDate,
    setStartDate: (LocalDate) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Icon(
            imageVector = vectorResource(Res.drawable.ic_today),
            contentDescription = stringResource(Res.string.select_date),
            tint = LocalAppColors.current.dark,
            modifier = iconModifier
        )

        Text(
            text = stringResource(Res.string.batch_record_start_date),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
        )

        SingleDatePicker(
            date = date,
            showIcon = false,
            fontSize = fontSize,
            modifier = Modifier
                .rippleClick { showDatePicker = true }
                .padding(vertical = 4.dp)
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            date = date,
            onDatePicked = setStartDate,
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun TimesSelector(
    times: Int,
    batchTimes: List<Int>,
    setTimes: (Int) -> Unit,
) {
    var isTimesMenuShown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Icon(
            imageVector = vectorResource(Res.drawable.ic_numbers),
            tint = LocalAppColors.current.dark,
            modifier = iconModifier
        )

        Text(
            text = stringResource(Res.string.batch_record_times),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .rippleClick { isTimesMenuShown = true }
                .padding(vertical = 4.dp)
        ) {

            Text(
                text = times.toString(),
                fontSize = fontSize,
            )

            Icon(
                imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                tint = LocalAppColors.current.dark
            )

            DropdownMenu(
                expanded = isTimesMenuShown,
                onDismissRequest = { isTimesMenuShown = false },
                modifier = Modifier.heightIn(max = 240.dp)
            ) {

                batchTimes.forEach { times ->
                    DropdownItem(
                        name = times.toString(),
                        onClick = {
                            setTimes(times)
                            isTimesMenuShown = false
                        }
                    )
                }
            }
        }
    }
}