package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme

val chartColors = listOf(
    Color(0xFFEEE3D2),
    Color(0xFFD8E3DC),
    Color(0xFFD6D9C1),
    Color(0xFFD7CFA8),
    Color(0xFFE1BC9E),
    Color(0xFFD3BCAF),
    Color(0xFFDABFC8),
    Color(0xFFC3B5C4),
    Color(0xFF9CA7C0),
    Color(0xFFB4BAC6),
)

/**
 *  For PieChart to avoid color collision when drawing back to the origin.
 */
val chartColorsStaggered = chartColors +
    chartColors.reversed().drop(2)

@Preview
@Composable
private fun ChartColors_BarChart_Preview() = AppTheme {
    Column(modifier = Modifier.background(LocalAppColors.current.light)) {
        chartColors.forEach { color ->
            OverviewGroup(
                state = OverviewGroupState.preview.copy(color = color)
            )
        }
    }
}

@Preview
@Composable
private fun ChartColors_PieChart_Preview() = AppTheme {
    val recordGroups = remember {
        var i = 0
        chartColors.associate {
            i++.toString() to listOf(Record(price = 1.0))
        }
    }

    val total = remember(recordGroups) {
        recordGroups.values.sumOf { list ->
            list.sumOf { it.price }
        }
    }

    PieChart(
        totalPrice = total,
        recordGroups = recordGroups,
        formatPrice = { it.toString() },
        highlightPieChart = {},
        onClick = {},
        modifier = Modifier.background(LocalAppColors.current.light)
    )
}