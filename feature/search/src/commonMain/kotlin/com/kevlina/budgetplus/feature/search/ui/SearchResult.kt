package com.kevlina.budgetplus.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.lottie.NotFoundZeroCase
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.InfiniteCircularProgress
import com.kevlina.budgetplus.feature.record.card.DeleteRecordDialog
import com.kevlina.budgetplus.feature.record.card.EditRecordDialog
import com.kevlina.budgetplus.feature.record.card.RecordCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn

@Composable
internal fun SearchResult(
    modifier: Modifier = Modifier,
    state: SearchResultState,
) {
    val result = state.result.collectAsStateWithLifecycle().value

    var editRecordDialog by remember { mutableStateOf<Record?>(null) }
    var deleteRecordDialog by remember { mutableStateOf<Record?>(null) }

    LaunchedEffect(state) {
        state.editRecordEvent.consumeEach { editRecordDialog = it }.launchIn(this)
        state.deleteRecordEvent.consumeEach { deleteRecordDialog = it }.launchIn(this)
    }

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        when (result) {
            SearchResult.Empty -> item(
                key = SearchResultUiType.ZeroCase.name,
                contentType = SearchResultUiType.ZeroCase,
                content = {
                    NotFoundZeroCase(modifier = Modifier.padding(top = 32.dp))
                }
            )

            SearchResult.Loading -> item(
                key = SearchResultUiType.Loader.name,
                contentType = SearchResultUiType.Loader,
                content = {
                    InfiniteCircularProgress(modifier = Modifier.padding(top = 32.dp))
                }
            )

            is SearchResult.Success -> items(
                items = result.records,
                key = { it.item.id },
                contentType = { SearchResultUiType.Record }
            ) { recordState ->
                RecordCard(
                    recordState
                )
            }
        }
    }

    editRecordDialog?.let { editRecord ->
        EditRecordDialog(
            editRecord = editRecord,
            onDismiss = { editRecordDialog = null }
        )
    }

    deleteRecordDialog?.let { deleteRecord ->
        DeleteRecordDialog(
            editRecord = deleteRecord,
            onDismiss = { deleteRecordDialog = null }
        )
    }
}

private enum class SearchResultUiType {
    Record, ZeroCase, Loader
}

@Preview
@Composable
private fun SearchResult_Preview(
    @PreviewParameter(SearchPreviewProvider::class) result: SearchResult,
) = AppTheme(ThemeColors.Dusk) {
    SearchResult(
        modifier = Modifier.background(LocalAppColors.current.light),
        state = SearchResultState.preview.copy(result = MutableStateFlow(result))
    )
}

private class SearchPreviewProvider : PreviewParameterProvider<SearchResult> {
    override val values: Sequence<SearchResult> = sequenceOf(
        SearchResult.Empty,
        SearchResult.Loading,
        SearchResultState.preview.result.value
    )
}