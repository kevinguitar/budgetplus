package com.kevlina.budgetplus.feature.insider.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Text
import org.jetbrains.compose.resources.vectorResource
import java.text.DecimalFormat

@Composable
internal fun StatCard(
    title: String,
    icon: ImageVector,
    number: Long,
) {

    val numberText = remember(number) {
        DecimalFormat.getInstance().format(number)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .clip(AppTheme.cardShape)
            .background(LocalAppColors.current.lightBg)
            .padding(16.dp)
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1F)
        ) {

            Image(
                imageVector = icon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(LocalAppColors.current.dark),
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = FontSize.SemiLarge
            )
        }

        Text(
            text = numberText,
            fontWeight = FontWeight.Bold,
            fontSize = FontSize.HeaderXLarge
        )
    }
}

@Preview
@Composable
private fun StatCard_Preview() = AppTheme {
    StatCard(
        title = "Total users",
        icon = vectorResource(Res.drawable.ic_people),
        number = 3429
    )
}