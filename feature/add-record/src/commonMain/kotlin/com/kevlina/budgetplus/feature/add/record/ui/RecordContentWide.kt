package com.kevlina.budgetplus.feature.add.record.ui

import androidx.compose.foundation.background
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
import com.kevlina.budgetplus.core.ui.WideContentRow

@Composable
internal fun RecordContentWide(
    state: RecordContentState,
    modifier: Modifier = Modifier,
) {
    WideContentRow(modifier = modifier.fillMaxSize()) {
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
                // Match the bottom padding of the record info next to it.
                .padding(top = 16.dp, bottom = 8.dp)
        )
    }
}

@Preview(widthDp = 800, heightDp = 360)
@Preview(widthDp = 960, heightDp = 400)
@Composable
private fun RecordContentWide_Preview() = AppTheme(themeColors = ThemeColors.AzureCoast) {
    RecordContentWide(
        state = RecordContentState(
            recordInfoState = RecordInfoState.preview,
            calculatorState = CalculatorState.preview,
        ),
        modifier = Modifier.background(LocalAppColors.current.light)
    )
}