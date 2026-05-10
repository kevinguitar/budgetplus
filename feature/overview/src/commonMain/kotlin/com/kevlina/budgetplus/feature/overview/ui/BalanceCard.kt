package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.overview_balance
import budgetplus.core.common.generated.resources.overview_total_price
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Text
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun BalanceCard(
    modifier: Modifier = Modifier,
    totalPrice: String,
    balance: String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(AppTheme.cardShape)
            .background(LocalAppColors.current.lightBg)
            .padding(horizontal = 16.dp)
    ) {

        BalanceItem(
            title = stringResource(Res.string.overview_total_price),
            balance = totalPrice,
            modifier = Modifier.weight(1F)
        )

        Spacer(modifier = Modifier
            .padding(horizontal = 16.dp)
            .width(2.dp)
            .fillMaxHeight()
            .background(LocalAppColors.current.light)
        )

        BalanceItem(
            title = stringResource(Res.string.overview_balance),
            balance = balance,
            modifier = Modifier.weight(1F)
        )
    }
}

@Composable
private fun BalanceItem(
    modifier: Modifier = Modifier,
    title: String,
    balance: String,
) {

    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {

        Text(
            text = title,
            color = LocalAppColors.current.dark,
        )

        Text(
            text = balance,
            color = LocalAppColors.current.dark,
            fontWeight = FontWeight.SemiBold,
            fontSize = FontSize.SemiLarge,
            singleLine = true
        )
    }
}

@Preview
@Composable
private fun BalanceCard_Preview() = AppTheme {
    BalanceCard(totalPrice = "$4,582.23", balance = "$120.64")
}