package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Switch
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.rippleClick

@Composable
internal fun CurrencyToggle(
    state: CurrencyToggleState,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .rippleClick(onClick = state.onClick)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = state.bookCurrencySymbol,
            fontWeight = FontWeight.SemiBold
        )

        Switch(
            checked = state.toggleState,
            onCheckedChange = { state.onClick() },
        )

        Text(
            text = state.preferredCurrencySymbol,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Immutable
internal data class CurrencyToggleState(
    val bookCurrencySymbol: String,
    val preferredCurrencySymbol: String,
    val toggleState: Boolean,
    val onClick: () -> Unit,
) {
    companion object {
        val preview = CurrencyToggleState(
            bookCurrencySymbol = "USD",
            preferredCurrencySymbol = "TWD",
            toggleState = true,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun CurrencyToggle_Preview() = AppTheme {
    CurrencyToggle(CurrencyToggleState.preview)
}