package com.kevlina.budgetplus.feature.overview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_search
import budgetplus.core.common.generated.resources.search_title
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.remote.Author
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.settings.api.ChartMode
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Fab
import com.kevlina.budgetplus.core.ui.InfiniteCircularProgress
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.bubble.BubbleShape
import com.kevlina.budgetplus.core.ui.thenIf
import com.kevlina.budgetplus.feature.overview.OverviewMode
import com.kevlina.budgetplus.feature.record.card.DeleteRecordDialog
import com.kevlina.budgetplus.feature.record.card.EditRecordDialog
import com.kevlina.budgetplus.feature.record.card.RecordCard
import com.kevlina.budgetplus.feature.record.card.RecordCardState
import com.kevlina.budgetplus.feature.record.card.RecordCardZeroCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun OverviewList(
    state: OverviewListState,
    navController: NavController<BookDest>,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
) {

    val mode by state.mode.collectAsStateWithLifecycle()
    val chartMode by state.chartMode.collectAsStateWithLifecycle()
    val type by state.type.collectAsStateWithLifecycle()
    val selectedAuthor by state.selectedAuthor.collectAsStateWithLifecycle()
    val totalPrice by state.totalPrice.collectAsStateWithLifecycle()
    val recordList = state.recordList.collectAsStateWithLifecycle().value
    val recordGroups by state.recordGroups.collectAsStateWithLifecycle()
    val isSoloAuthor by state.isSoloAuthor.collectAsStateWithLifecycle()

    var editRecordDialog by remember { mutableStateOf<Record?>(null) }
    var deleteRecordDialog by remember { mutableStateOf<Record?>(null) }
    var isSearchFabVisible by rememberSaveable { mutableStateOf(true) }

    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress || listState.lastScrolledForward }
            .collect { isSearchFabVisible = !it }
    }

    fun navigateToRecords(category: String) {
        state.onGroupClicked()
        navController.navigate(
            BookDest.Records(
                type = type,
                category = category,
                authorId = selectedAuthor?.id
            )
        )
    }

    Box(modifier) {
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (header != null) {
                item(
                    key = OverviewUiType.Header.name,
                    contentType = OverviewUiType.Header,
                    content = { header() }
                )
            }

            when {
                recordList == null -> item(
                    key = OverviewUiType.Loader.name,
                    contentType = OverviewUiType.Loader,
                    content = {
                        InfiniteCircularProgress(modifier = Modifier.padding(top = 32.dp))
                    }
                )

                recordList.isEmpty() -> item(
                    key = OverviewUiType.ZeroCase.name,
                    contentType = OverviewUiType.ZeroCase,
                    content = {
                        RecordCardZeroCase(modifier = Modifier.padding(top = 32.dp))
                    }
                )

                else -> when (mode) {
                    OverviewMode.AllRecords -> itemsIndexed(
                        items = recordList,
                        key = { _, record -> record.id },
                        contentType = { _, _ -> OverviewUiType.Record }
                    ) { index, record ->
                        RecordCard(
                            state = RecordCardState(
                                item = record,
                                formattedPrice = state.formatPrice(record.price),
                                isLast = index == recordList.lastIndex,
                                canEdit = state.canEditRecord(record),
                                showCategory = true,
                                showAuthor = !isSoloAuthor,
                                onEdit = { editRecordDialog = record },
                                onDuplicate = { state.duplicateRecord(record) },
                                onDelete = { deleteRecordDialog = record }
                            ),
                            modifier = Modifier.thenIf(index == 0) {
                                val bubbleShape = with(LocalDensity.current) {
                                    BubbleShape.RoundedRect(AppTheme.cornerRadius.toPx())
                                }

                                Modifier
                                    .padding(top = 8.dp)
                                    .onPlaced {
                                        state.highlightTapHint(BubbleDest.OverviewRecordTapHint(
                                            size = it.size,
                                            offset = it::positionInRoot,
                                            shape = bubbleShape
                                        ))
                                    }
                            }
                        )
                    }

                    OverviewMode.GroupByCategories -> when (chartMode) {
                        ChartMode.BarChart -> itemsIndexed(
                            items = recordGroups?.keys?.toList().orEmpty(),
                            key = { _, key -> key },
                            contentType = { _, _ -> OverviewUiType.Group }
                        ) { index, key ->

                            val groupRecords = recordGroups.orEmpty()[key].orEmpty()
                            val sum = groupRecords.sumOf { it.price }

                            OverviewGroup(
                                modifier = Modifier.thenIf(index == 0) {
                                    Modifier.padding(top = 8.dp)
                                },
                                state = OverviewGroupState(
                                    category = key,
                                    records = groupRecords,
                                    sumPrice = state.formatPrice(sum),
                                    percentage = remember(sum, totalPrice) { sum / totalPrice },
                                    color = chartColors[index % chartColors.size],
                                    isLast = index == recordGroups.orEmpty().size - 1,
                                    onClick = { navigateToRecords(key) }
                                ),
                            )
                        }

                        ChartMode.PieChart -> item(
                            key = OverviewUiType.PieChart.name,
                            contentType = OverviewUiType.PieChart,
                            content = {
                                PieChart(
                                    modifier = Modifier.fillMaxSize(),
                                    totalPrice = totalPrice,
                                    recordGroups = recordGroups.orEmpty(),
                                    formatPrice = state.formatPrice,
                                    highlightPieChart = state.highlightPieChart,
                                    onClick = ::navigateToRecords
                                )
                            }
                        )
                    }
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

        Fab(
            isVisible = isSearchFabVisible,
            icon = vectorResource(Res.drawable.ic_search),
            contentDescription = stringResource(Res.string.search_title),
            onClick = { navController.navigate(BookDest.Search(type)) }
        )
    }
}

@Stable
internal data class OverviewListState(
    val mode: StateFlow<OverviewMode>,
    val chartMode: StateFlow<ChartMode>,
    val type: StateFlow<RecordType>,
    val selectedAuthor: StateFlow<User?>,
    val totalPrice: StateFlow<Double>,
    val recordList: StateFlow<List<Record>?>,
    val recordGroups: StateFlow<Map<String, List<Record>>?>,
    val isSoloAuthor: StateFlow<Boolean>,
    val highlightTapHint: (BubbleDest) -> Unit,
    val highlightPieChart: (BubbleDest) -> Unit,
    val formatPrice: (Double) -> String,
    val canEditRecord: (Record) -> Boolean,
    val duplicateRecord: (Record) -> Unit,
    val onGroupClicked: () -> Unit,
) {
    companion object {

        private val foodRecords = listOf(
            Record(category = "Food", name = "High Tea", price = 45.3, author = Author(name = "Kevin"), id = "1"),
            Record(category = "Food", name = "Steak", price = 9.3, author = Author(name = "Kevin"), id = "2"),
            Record(category = "Food", name = "Snack", price = 4.7, author = Author(name = "Alina"), id = "3"),
            Record(category = "Food", name = "Mix Vegi rice", price = 12.9, author = Author(name = "Alina"), id = "4"),
        )

        val recordGroupsPreview = mapOf(
            "Loan" to listOf(Record(category = "Loan", price = 433.0, author = Author(name = "Kevin"))),
            "Daily" to listOf(Record(category = "Daily", price = 342.1, author = Author(name = "Alina"))),
            "Utility" to listOf(Record(category = "Utility", price = 132.5, author = Author(name = "Kevin"))),
            "Food" to foodRecords,
        )

        val totalPricePreview = recordGroupsPreview.values
            .sumOf { group -> group.sumOf { it.price } }

        val preview = OverviewListState(
            mode = MutableStateFlow(OverviewMode.GroupByCategories),
            chartMode = MutableStateFlow(ChartMode.BarChart),
            type = MutableStateFlow(RecordType.Expense),
            selectedAuthor = MutableStateFlow(User(name = "Kevin")),
            totalPrice = MutableStateFlow(totalPricePreview),
            recordList = MutableStateFlow(foodRecords),
            recordGroups = MutableStateFlow(recordGroupsPreview),
            isSoloAuthor = MutableStateFlow(false),
            highlightTapHint = {},
            highlightPieChart = {},
            formatPrice = { "$$it" },
            canEditRecord = { true },
            duplicateRecord = {},
            onGroupClicked = {}
        )
    }
}

@Preview
@Composable
private fun OverviewList_All_Preview() = AppTheme {
    OverviewList(
        state = OverviewListState.preview.copy(
            mode = MutableStateFlow(OverviewMode.AllRecords)
        ),
        navController = NavController.preview,
        modifier = Modifier.background(LocalAppColors.current.light)
    )
}

@Preview
@Composable
private fun OverviewList_Group_Preview() = AppTheme {
    OverviewList(
        state = OverviewListState.preview,
        navController = NavController.preview,
        modifier = Modifier.background(LocalAppColors.current.light)
    )
}

@Preview
@Composable
private fun OverviewList_PieChart_Preview() = AppTheme {
    OverviewList(
        state = OverviewListState.preview.copy(
            mode = MutableStateFlow(OverviewMode.GroupByCategories),
            chartMode = MutableStateFlow(ChartMode.PieChart)
        ),
        navController = NavController.preview,
        modifier = Modifier.background(LocalAppColors.current.light)
    )
}