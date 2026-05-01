package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors

@Composable
fun PagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    selectedColor: Color = LocalAppColors.current.dark,
    unselectedColor: Color = LocalAppColors.current.primary.copy(alpha = 0.5F),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {

        repeat(pagerState.pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) {
                selectedColor
            } else {
                unselectedColor
            }
            Spacer(
                modifier = Modifier
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(6.dp)
            )
        }
    }
}

@Preview
@Composable
private fun PagerIndicator_Preview() = AppTheme {
    PagerIndicator(pagerState = rememberPagerState { 8 })
}