package com.kevlina.budgetplus.feature.unlock.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.premium_description
import budgetplus.core.common.generated.resources.premium_free_trial_cta
import budgetplus.core.common.generated.resources.premium_plan_annual
import budgetplus.core.common.generated.resources.premium_plan_lifetime
import budgetplus.core.common.generated.resources.premium_plan_monthly
import budgetplus.core.common.generated.resources.premium_restore_purchase
import budgetplus.core.common.generated.resources.premium_unlock
import com.kevlina.budgetplus.core.billing.PremiumPlan
import com.kevlina.budgetplus.core.billing.Pricing
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Button
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.containerPadding
import com.kevlina.budgetplus.core.ui.rippleClick
import org.jetbrains.compose.resources.stringResource

@Composable
fun PremiumContent(
    pricingMap: Map<PremiumPlan, Pricing?>,
    purchase: (PremiumPlan) -> Unit,
    restorePurchases: () -> Unit,
) {
    Column(
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
            text = stringResource(Res.string.premium_description),
            fontSize = FontSize.SemiLarge,
            lineHeight = 32.sp,
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp)
        )

        var selectedPlan by remember { mutableStateOf(PremiumPlan.Annual) }

        pricingMap.forEach { (plan, pricing) ->
            val planRes = when (plan) {
                PremiumPlan.Monthly -> Res.string.premium_plan_monthly
                PremiumPlan.Annual -> Res.string.premium_plan_annual
                PremiumPlan.Lifetime -> Res.string.premium_plan_lifetime
            }
            PremiumPlanCell(
                planRes = planRes,
                pricing = pricing,
                isSelected = plan == selectedPlan,
                onClick = { selectedPlan = plan }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            enabled = pricingMap[selectedPlan] != null,
            onClick = { purchase(selectedPlan) }
        ) {
            val pricing = pricingMap[selectedPlan]
            val text = when {
                pricing == null -> stringResource(Res.string.premium_unlock)
                (pricing.freeTrialDays ?: 0) > 0 -> {
                    stringResource(Res.string.premium_free_trial_cta, pricing.freeTrialDays.toString())
                }

                else -> stringResource(Res.string.premium_unlock)
            }
            Text(
                text = text,
                color = LocalAppColors.current.light,
                fontSize = FontSize.Large,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.premium_restore_purchase),
            fontSize = FontSize.Small,
            modifier = Modifier
                .rippleClick(onClick = restorePurchases)
                .padding(all = 4.dp)
        )
    }
}

@Preview
@Composable
private fun PremiumContent_Preview() = AppTheme {
    PremiumContent(
        pricingMap = mapOf(
            PremiumPlan.Monthly to Pricing(null, "$10", 3),
            PremiumPlan.Annual to Pricing(null, "$100", 7),
            PremiumPlan.Lifetime to Pricing(null, "$499", null)
        ),
        purchase = {},
        restorePurchases = {}
    )
}