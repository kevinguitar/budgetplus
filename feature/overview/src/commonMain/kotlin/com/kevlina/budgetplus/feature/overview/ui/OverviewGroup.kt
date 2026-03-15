package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.common.roundUpRatioText
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.withTypographyScale
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.darken
import com.kevlina.budgetplus.core.ui.rippleClick

private const val TEXT_DARKEN_FACTOR = 0.7F
private const val A_HUNDRED = 100

@Composable
internal fun OverviewGroup(
    state: OverviewGroupState,
    modifier: Modifier = Modifier,
) {
    val percentage = state.percentage

    Box(
        modifier = modifier
            .fillMaxWidth()
            .rippleClick(onClick = state.onClick)
            .padding(horizontal = 16.dp)
    ) {

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1F)
                    .padding(vertical = 16.dp)
            ) {

                val progress = remember(percentage) {
                    calcProgressWidth(maxWidth, percentage)
                }
                val width by animateDpAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 500),
                    label = "widthAnimation_${state.category}"
                )

                Spacer(
                    modifier = Modifier
                        .width(width)
                        .height(24.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            color = state.color.copy(alpha = 0.5F),
                            shape = RoundedCornerShape(
                                topEndPercent = 50,
                                bottomEndPercent = 50
                            )
                        )
                )

                Text(
                    text = state.category,
                    fontSize = FontSize.SemiLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                Text(
                    text = state.sumPrice,
                    fontSize = FontSize.SemiLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            Text(
                text = "${(percentage * A_HUNDRED).roundUpRatioText}%",
                color = LocalAppColors.current.light,
                fontSize = FontSize.Small,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(
                        color = state.color
                            .darken(TEXT_DARKEN_FACTOR)
                            .copy(alpha = 0.7F),
                        shape = CircleShape
                    )
                    .width(48.dp.withTypographyScale())
                    .padding(vertical = 4.dp.withTypographyScale())
            )
        }

        if (!state.isLast) {
            Spacer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(color = LocalAppColors.current.primary)
            )
        }
    }
}

private const val PROGRESS_AMP = 0.2F

private fun calcProgressWidth(maxWidth: Dp, ratio: Double): Dp {
    val base = maxWidth * ratio.toFloat()
    val amp = (maxWidth - base) * PROGRESS_AMP
    return base + amp
}

@Immutable
internal data class OverviewGroupState(
    val category: String,
    val records: List<Record>,
    val color: Color,
    val isLast: Boolean,
    val sumPrice: String,
    val percentage: Double,
    val onClick: () -> Unit,
) {
    companion object {
        val preview = OverviewGroupState(
            category = "日常",
            records = listOf(
                Record(price = 10.3),
                Record(price = 42.43),
                Record(price = 453.1),
            ),
            sumPrice = "$1043.5",
            percentage = 0.34,
            color = chartColors[0],
            isLast = false,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OverviewGroup_Preview() = AppTheme {
    OverviewGroup(OverviewGroupState.preview)
}