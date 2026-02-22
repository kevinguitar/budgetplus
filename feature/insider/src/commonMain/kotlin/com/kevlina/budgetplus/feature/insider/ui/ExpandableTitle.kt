package com.kevlina.budgetplus.feature.insider.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_keyboard_arrow_down
import budgetplus.core.common.generated.resources.ic_keyboard_arrow_up
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.rippleClick
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun ExpandableTitle(
    title: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
            .rippleClick(onClick = onClick)
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = FontSize.Large,
            modifier = Modifier.weight(1F)
        )

        Icon(
            imageVector = if (isExpanded) vectorResource(Res.drawable.ic_keyboard_arrow_down) else vectorResource(Res.drawable.ic_keyboard_arrow_up),
            tint = LocalAppColors.current.dark,
        )
    }
}