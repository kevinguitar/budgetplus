package com.kevlina.budgetplus.feature.unlock.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.premium_plan_monthly
import com.kevlina.budgetplus.core.billing.Pricing
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.InfiniteCircularProgress
import com.kevlina.budgetplus.core.ui.Surface
import com.kevlina.budgetplus.core.ui.Text
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PremiumPlanCell(
    planRes: StringResource,
    pricing: Pricing?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        enabled = pricing != null,
        border = BorderStroke(1.dp, if (isSelected) LocalAppColors.current.primary else LocalAppColors.current.lightBg),
        shape = AppTheme.cardShape,
        color = LocalAppColors.current.light,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSelected) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.CheckCircleOutline
                },
                tint = LocalAppColors.current.primary
            )

            Text(
                text = stringResource(planRes),
                color = LocalAppColors.current.dark,
                fontSize = FontSize.Large,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1F))

            if (pricing?.formattedPrice == null) {
                InfiniteCircularProgress(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 1.dp
                )
            } else {
                Text(
                    text = pricing.formattedPrice,
                    color = LocalAppColors.current.dark,
                    fontSize = FontSize.Large,
                    fontWeight = FontWeight.Medium,
                )

                //TODO: Discounted price should be displayed here
            }
        }
    }
}

@Preview
@Composable
private fun PremiumPlanCell_Preview() = AppTheme {
    PremiumPlanCell(
        planRes = Res.string.premium_plan_monthly,
        isSelected = true,
        pricing = Pricing(
            discountedPrice = null,
            formattedPrice = "$4.99",
            freeTrialDays = 7,
        ),
    ) { }
}