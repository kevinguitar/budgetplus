package com.kevlina.budgetplus.feature.add.record.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppTheme

@Composable
internal fun RecordContentWide(
    state: RecordContentState,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
    ) {
        RecordInfo(
            state = state.recordInfoState,
            modifier = Modifier.weight(1F)
        )

        Calculator(
            state = state.calculatorState,
            adaptiveButton = true,
            modifier = Modifier
                .weight(1F)
                .fillMaxHeight()
                .padding(vertical = 16.dp)
        )
    }
}

@Preview(widthDp = 800, heightDp = 360)
@Preview(widthDp = 960, heightDp = 400)
@Composable
private fun RecordContentWide_Preview() = AppTheme(themeColors = ThemeColors.NemoSea) {
    RecordContentWide(
        state = RecordContentState(
            recordInfoState = RecordInfoState.preview,
            calculatorState = CalculatorState.preview,
        ),
        modifier = Modifier.background(LocalAppColors.current.light)
    )
}