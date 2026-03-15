package com.kevlina.budgetplus.feature.add.record.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.thenIf

private const val DISABLE_ADAPTIVE_BUTTON_RATIO = 1.2

@Composable
internal fun RecordContentRegular(
    state: RecordContentState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints {
        val useAdaptiveButton = maxHeight / maxWidth < DISABLE_ADAPTIVE_BUTTON_RATIO && maxHeight < 800.dp
        val maxCalculatorHeight = maxHeight / 2

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.fillMaxHeight()
        ) {
            RecordInfo(
                state = state.recordInfoState,
                modifier = Modifier
                    .weight(1F)
                    .width(AppTheme.containerMaxWidth)
                    .padding(horizontal = 16.dp)
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .alpha(AppTheme.DIVIDER_ALPHA)
                    .background(color = LocalAppColors.current.dark)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .thenIf(useAdaptiveButton) {
                        Modifier.height(maxCalculatorHeight)
                    }
                    .background(LocalAppColors.current.lightBg)
            ) {
                Calculator(
                    state = state.calculatorState,
                    adaptiveButton = useAdaptiveButton,
                    modifier = Modifier
                        .width(AppTheme.containerMaxWidth)
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                        .background(
                            color = LocalAppColors.current.light,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 420)
@Preview(widthDp = 360, heightDp = 640)
@Preview(widthDp = 480, heightDp = 640)
@Preview(widthDp = 480, heightDp = 840)
@Preview(widthDp = 600, heightDp = 480)
@Preview(widthDp = 1080, heightDp = 840)
@Composable
private fun RecordContentRegular_Preview() = AppTheme(themeColors = ThemeColors.Barbie) {
    RecordContentRegular(
        state = RecordContentState.preview,
        modifier = Modifier.background(LocalAppColors.current.light)
    )
}