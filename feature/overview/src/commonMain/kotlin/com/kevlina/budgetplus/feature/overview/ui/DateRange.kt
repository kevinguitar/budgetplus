package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.date_to
import budgetplus.core.common.generated.resources.ic_chevron_left
import budgetplus.core.common.generated.resources.ic_chevron_right
import budgetplus.core.common.generated.resources.ic_date_range
import budgetplus.core.common.generated.resources.select_date
import com.kevlina.budgetplus.core.common.fullFormatted
import com.kevlina.budgetplus.core.common.mediumFormatted
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.rippleClick
import com.kevlina.budgetplus.core.ui.thenIf
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun DateRange(
    state: TimePeriodSelectorState,
    showDateRangePicker: () -> Unit,
) {

    val fromDate by state.fromDate.collectAsStateWithLifecycle()
    val untilDate by state.untilDate.collectAsStateWithLifecycle()
    val isOneDayPeriod by state.isOneDayPeriod.collectAsStateWithLifecycle()

    val arrowPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        modifier = Modifier.rippleClick(onClick = showDateRangePicker)
    ) {
        if (isOneDayPeriod) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_chevron_left),
                tint = LocalAppColors.current.dark,
                modifier = Modifier
                    .padding(arrowPadding)
                    .rippleClick(borderless = true, onClick = state.previousDay)
            )
        }

        Icon(
            imageVector = vectorResource(Res.drawable.ic_date_range),
            contentDescription = stringResource(Res.string.select_date),
            tint = LocalAppColors.current.dark
        )

        Text(
            text = if (isOneDayPeriod) {
                fromDate.fullFormatted
            } else {
                fromDate.mediumFormatted
            },
            singleLine = true,
            modifier = Modifier
                .padding(all = 8.dp)
                .thenIf(isOneDayPeriod) {
                    Modifier.weight(1F, fill = false)
                }
        )

        if (isOneDayPeriod) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_chevron_right),
                tint = LocalAppColors.current.dark,
                modifier = Modifier
                    .padding(arrowPadding)
                    .rippleClick(borderless = true, onClick = state.nextDay)
            )
        } else {
            Text(text = stringResource(Res.string.date_to))

            Text(
                text = untilDate.mediumFormatted,
                singleLine = true,
                modifier = Modifier
                    .padding(all = 8.dp)
                    .weight(1F, fill = false)
            )
        }
    }
}