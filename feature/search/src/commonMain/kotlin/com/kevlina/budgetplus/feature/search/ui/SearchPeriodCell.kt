package com.kevlina.budgetplus.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_radio_button_checked
import budgetplus.core.common.generated.resources.ic_radio_button_unchecked
import budgetplus.core.common.generated.resources.search_period_custom
import budgetplus.core.common.generated.resources.search_period_custom_range
import budgetplus.core.common.generated.resources.search_period_last_half_year
import budgetplus.core.common.generated.resources.search_period_last_month
import budgetplus.core.common.generated.resources.search_period_last_year
import com.kevlina.budgetplus.core.common.shortFormatted
import com.kevlina.budgetplus.core.lottie.PremiumCrown
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.rippleClick
import com.kevlina.budgetplus.feature.search.ui.SearchPeriod.Companion.requiresPremium
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun SearchPeriodCell(
    modifier: Modifier = Modifier,
    period: SearchPeriod,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .rippleClick(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Icon(
            imageVector = if (isSelected) {
                vectorResource(Res.drawable.ic_radio_button_checked)
            } else {
                vectorResource(Res.drawable.ic_radio_button_unchecked)
            },
            tint = LocalAppColors.current.dark,
            modifier = Modifier
        )

        Text(
            text = period.text(),
            fontSize = FontSize.SemiLarge,
            modifier = Modifier.weight(1F)
        )

        if (period.requiresPremium) {
            PremiumCrown(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
internal fun SearchPeriod.text(): String {
    return when (this) {
        SearchPeriod.PastMonth -> stringResource(Res.string.search_period_last_month)
        SearchPeriod.PastHalfYear -> stringResource(Res.string.search_period_last_half_year)
        SearchPeriod.PastYear -> stringResource(Res.string.search_period_last_year)
        is SearchPeriod.Custom -> {
            if (this === SearchPeriod.Custom.Unselected) {
                stringResource(Res.string.search_period_custom)
            } else {
                stringResource(
                    Res.string.search_period_custom_range,
                    from.shortFormatted,
                    until.shortFormatted
                )
            }
        }
    }
}

@Preview
@Composable
private fun SearchPeriodCell_Preview() = AppTheme {
    Column(
        modifier = Modifier
            .background(LocalAppColors.current.light)
            .padding(vertical = 16.dp)
    ) {
        SearchPeriodCell(
            period = SearchPeriod.PastMonth,
            isSelected = true,
            onClick = {}
        )
        SearchPeriodCell(
            period = SearchPeriod.PastYear,
            isSelected = false,
            onClick = {}
        )
        SearchPeriodCell(
            period = SearchPeriod.Custom.Unselected,
            isSelected = false,
            onClick = {}
        )
    }
}