package com.kevlina.budgetplus.feature.add.record.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.WideContentRow
import com.kevlina.budgetplus.core.ui.safeArea

private val CalculatorTopPadding = 16.dp
private val CalculatorMinBottomPadding = 16.dp

/**
 *  The keypad may take at most this fraction of the width, so the form always keeps enough room.
 */
private const val MAX_KEYPAD_WIDTH_FRACTION = 0.55F

@Composable
internal fun RecordContentWide(
    state: RecordContentState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val bottomInsets = WindowInsets.safeArea.only(WindowInsetsSides.Bottom)
            .union(WindowInsets(bottom = CalculatorMinBottomPadding))
        val bottomPadding = with(LocalDensity.current) { bottomInsets.getBottom(this).toDp() }

        // Size the keypad from the available height with round keys, the form gets the rest.
        val availableHeight = maxHeight - CalculatorTopPadding - bottomPadding
        val keySize = ((availableHeight - CalculatorVerticalSpacing * (CALCULATOR_ROWS - 1)) / CALCULATOR_ROWS)
            .coerceAtLeast(0.dp)
        val keypadWidth = (keySize * CALCULATOR_COLUMNS + CalculatorHorizontalSpacing * (CALCULATOR_COLUMNS - 1))
            .coerceAtMost(maxWidth * MAX_KEYPAD_WIDTH_FRACTION)

        WideContentRow(modifier = Modifier.fillMaxSize()) {
            RecordInfo(
                state = state.recordInfoState,
                modifier = Modifier.weight(1F)
            )

            Calculator(
                state = state.calculatorState,
                adaptiveButton = false,
                modifier = Modifier
                    .width(keypadWidth)
                    .fillMaxHeight()
                    .padding(top = CalculatorTopPadding)
                    // Keep the keys above the home indicator when there's no bottom nav below.
                    .windowInsetsPadding(bottomInsets)
                    .wrapContentHeight(Alignment.Bottom)
            )
        }
    }
}

@Preview(widthDp = 800, heightDp = 360)
@Preview(widthDp = 960, heightDp = 400)
@Preview(widthDp = 1280, heightDp = 800)
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
