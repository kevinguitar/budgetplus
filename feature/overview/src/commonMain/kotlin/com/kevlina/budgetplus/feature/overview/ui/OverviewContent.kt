package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme

@Composable
internal fun OverviewContent(
    state: OverviewContentState,
    navController: NavController<BookDest>,
    modifier: Modifier = Modifier,
) {
    OverviewList(
        state = state.listState,
        navController = navController,
        header = {
            OverviewHeader(
                state = state.headerState,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        },
        modifier = modifier.fillMaxSize()
    )
}

@Immutable
internal class OverviewContentState(
    val headerState: OverviewHeaderState,
    val listState: OverviewListState,
) {
    companion object {
        val preview = OverviewContentState(
            headerState = OverviewHeaderState.preview,
            listState = OverviewListState.preview
        )
    }
}

@Preview
@Composable
private fun OverviewContent_Preview() = AppTheme {
    OverviewContent(
        state = OverviewContentState.preview,
        navController = NavController.preview,
        modifier = Modifier.background(LocalAppColors.current.light)
    )
}