package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_today
import budgetplus.core.common.generated.resources.select_date
import budgetplus.core.common.generated.resources.today
import budgetplus.core.common.generated.resources.tomorrow
import budgetplus.core.common.generated.resources.yesterday
import com.kevlina.budgetplus.core.common.mediumFormatted
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun SingleDatePicker(
    date: LocalDate,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    fontSize: TextUnit = FontSize.Normal,
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {

        if (showIcon) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_today),
                contentDescription = stringResource(Res.string.select_date),
                tint = LocalAppColors.current.dark
            )
        }

        Text(
            text = when {
                date == LocalDate.now() -> stringResource(Res.string.today)
                date.plus(1, DateTimeUnit.DAY) == LocalDate.now() -> {
                    stringResource(Res.string.yesterday)
                }

                date.minus(1, DateTimeUnit.DAY) == LocalDate.now() -> {
                    stringResource(Res.string.tomorrow)
                }

                else -> date.mediumFormatted
            },
            fontSize = fontSize
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SingleDatePicker_Preview() = AppTheme(themeColors = ThemeColors.Barbie) {
    SingleDatePicker(
        date = LocalDate.now(),
        modifier = Modifier.padding(8.dp)
    )
}