package com.kevlina.budgetplus.feature.unlock.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.premium_description
import budgetplus.core.common.generated.resources.premium_unlock
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Button
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.InfiniteCircularProgress
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.containerPadding
import org.jetbrains.compose.resources.stringResource

@Composable
fun PremiumContent(
    monthlyPrice: String?,
    annualPrice: String?,
    buyMonthly: () -> Unit,
    buyAnnual: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color = LocalAppColors.current.light,
                shape = AppTheme.dialogShape
            )
            .verticalScroll(rememberScrollState())
            .containerPadding()
            .padding(16.dp)
    ) {

        InvestAnimation(modifier = Modifier.size(280.dp, 200.dp))

        Text(
            text = stringResource(Res.string.premium_unlock),
            fontSize = FontSize.HeaderLarge,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = stringResource(Res.string.premium_description),
            fontSize = FontSize.SemiLarge,
            lineHeight = 32.sp,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
        )

        if (monthlyPrice == null || annualPrice == null) {
            InfiniteCircularProgress(
                modifier = Modifier.size(32.dp),
                strokeWidth = 2.dp
            )
        } else {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                onClick = buyMonthly
            ) {
                Text(
                    text = monthlyPrice,
                    color = LocalAppColors.current.light,
                    fontSize = FontSize.Large,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                onClick = buyAnnual
            ) {
                Text(
                    text = annualPrice,
                    color = LocalAppColors.current.light,
                    fontSize = FontSize.Large,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumContent_Preview() = AppTheme {
    PremiumContent(monthlyPrice = "$10", annualPrice = "$100", buyMonthly = {}, buyAnnual = {})
}