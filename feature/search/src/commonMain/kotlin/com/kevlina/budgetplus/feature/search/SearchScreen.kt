package com.kevlina.budgetplus.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.search_title
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.TopBar
import com.kevlina.budgetplus.feature.search.ui.SearchContent
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchScreen(
    vm: SearchViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.light)
    ) {
        TopBar(
            title = stringResource(Res.string.search_title),
            navigateUp = vm.navController::navigateUp,
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
        ) {
            SearchContent(state = vm.state)
        }
    }
}