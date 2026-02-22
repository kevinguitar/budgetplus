package com.kevlina.budgetplus.feature.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_event_note
import budgetplus.core.common.generated.resources.ic_paid
import budgetplus.core.common.generated.resources.overview_details_title
import budgetplus.core.common.generated.resources.overview_sort_by_date
import budgetplus.core.common.generated.resources.overview_sort_by_price
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.MenuAction
import com.kevlina.budgetplus.core.ui.TopBar
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.feature.record.card.DeleteRecordDialog
import com.kevlina.budgetplus.feature.record.card.EditRecordDialog
import com.kevlina.budgetplus.feature.record.card.RecordCard
import com.kevlina.budgetplus.feature.record.card.RecordCardState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun RecordsScreen(
    navController: NavController<BookDest>,
    vm: RecordsViewModel,
) {
    var editRecordDialog by remember { mutableStateOf<Record?>(null) }
    var deleteRecordDialog by remember { mutableStateOf<Record?>(null) }

    val recordsList by vm.recordsList.collectAsStateWithLifecycle()
    val sortMode by vm.sortMode.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val totalPrice by vm.totalPrice.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(initialPage = vm.initialPage) { vm.pageSize }

    LaunchedEffect(key1 = pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect(vm::setPageIndex)
    }

    // Observe the records, when the last record get deleted, navigate back to the overview screen.
    LaunchedEffect(key1 = vm) {
        combine(
            vm.recordsList,
            snapshotFlow { pagerState.currentPage }
        ) { recordsList, page ->
            if (recordsList?.getOrNull(page)?.isEmpty() == true) {
                navController.navigateUp()
            }
        }.collect()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.light)
    ) {

        TopBar(
            title = stringResource(Res.string.overview_details_title, category, totalPrice),
            navigateUp = navController::navigateUp,
            menuActions = {
                val modifier = Modifier.onPlaced {
                    vm.highlightSortingButton(
                        BubbleDest.RecordsSorting(
                            size = it.size,
                            offset = it::positionInRoot
                        )
                    )
                }

                when (sortMode) {
                    RecordsSortMode.Date -> MenuAction(
                        imageVector = vectorResource(Res.drawable.ic_event_note),
                        description = stringResource(Res.string.overview_sort_by_price),
                        onClick = { vm.setSortMode(RecordsSortMode.Price) },
                        modifier = modifier
                    )

                    RecordsSortMode.Price -> MenuAction(
                        imageVector = vectorResource(Res.drawable.ic_paid),
                        description = stringResource(Res.string.overview_sort_by_date),
                        onClick = { vm.setSortMode(RecordsSortMode.Date) },
                        modifier = modifier
                    )
                }
            }
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
        ) {
            HorizontalPager(
                state = pagerState,
            ) { page ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = AppTheme.listContentPaddings()
                ) {
                    val records = recordsList?.getOrNull(page).orEmpty()
                    itemsIndexed(records) { index, item ->
                        RecordCard(state = RecordCardState(
                            item = item,
                            formattedPrice = vm.bookRepo.formatPrice(item.price),
                            isLast = index == records.lastIndex,
                            canEdit = vm.canEditRecord(item),
                            showCategory = false,
                            showAuthor = true,
                            onEdit = { editRecordDialog = item },
                            onDuplicate = { vm.duplicateRecord(item) },
                            onDelete = { deleteRecordDialog = item }
                        ))
                    }
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
}