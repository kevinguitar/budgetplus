package com.kevlina.budgetplus.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.search_field_placeholder
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.SearchField
import com.kevlina.budgetplus.core.ui.containerPadding
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SearchContent(
    modifier: Modifier = Modifier,
    state: SearchState,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .containerPadding()
    ) {
        val focusManager = LocalFocusManager.current

        SearchField(
            keyword = state.query,
            hint = stringResource(Res.string.search_field_placeholder),
            onDone = { focusManager.clearFocus() },
            modifier = Modifier
                .containerPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        SearchFilter(state = state.filter)

        Box(modifier = Modifier.weight(1F)) {
            SearchResult(state = state.result)

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(
                        SEARCH_GRADIENT_START to LocalAppColors.current.light,
                        SEARCH_GRADIENT_END to Color.Transparent
                    ))
            )
        }
    }
}

private const val SEARCH_GRADIENT_START = 0.4F
private const val SEARCH_GRADIENT_END = 1F

@PreviewScreenSizes
@Composable
private fun SearchContent_Preview() = AppTheme(themeColors = ThemeColors.Dusk) {
    SearchContent(
        state = SearchState.preview,
        modifier = Modifier.background(LocalAppColors.current.light)
    )
}