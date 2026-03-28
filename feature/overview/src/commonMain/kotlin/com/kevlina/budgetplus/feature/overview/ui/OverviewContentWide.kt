package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme

@Composable
internal fun OverviewContentWide(
    state: OverviewContentState,
    navController: NavController<BookDest>,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
    ) {
        OverviewHeader(
            state = state.headerState,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1F)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        )

        OverviewList(
            state = state.listState,
            navController = navController,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1F)
        )
    }
}

@Preview(widthDp = 800, heightDp = 300)
@Composable
private fun OverviewContentWide_Preview() = AppTheme {
    OverviewContentWide(
        state = OverviewContentState.preview,
        navController = NavController.preview,
        modifier = Modifier.background(LocalAppColors.current.light)
    )
}