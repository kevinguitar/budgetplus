package com.kevlina.budgetplus.feature.record.widget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.app_name
import budgetplus.core.common.generated.resources.cta_complete
import budgetplus.core.common.generated.resources.ic_check
import budgetplus.core.common.generated.resources.ic_check_circle
import budgetplus.core.common.generated.resources.ic_check_circle_outline
import budgetplus.core.common.generated.resources.record_income
import budgetplus.core.common.generated.resources.record_widget_book
import budgetplus.core.common.generated.resources.record_widget_empty
import budgetplus.core.common.generated.resources.record_widget_hint
import budgetplus.core.common.generated.resources.record_widget_preset_saved
import budgetplus.core.common.generated.resources.record_widget_times
import budgetplus.core.common.generated.resources.record_widget_title
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.UiTestFlags
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.ConfirmDialog
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.InfiniteCircularProgress
import com.kevlina.budgetplus.core.ui.MenuAction
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.TopBar
import com.kevlina.budgetplus.core.ui.rippleClick
import com.kevlina.budgetplus.core.ui.thenIf
import com.kevlina.budgetplus.feature.record.widget.FrequentItem
import com.kevlina.budgetplus.feature.record.widget.RECORD_WIDGET_MAX_ITEMS
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetItem
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetUiState
import com.kevlina.budgetplus.feature.record.widget.RecordWidgetViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collect
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/**
 * The entry from the app's settings.
 */
@Composable
fun RecordWidgetScreen() {
    val vm = metroViewModel<RecordWidgetViewModel>()
    var showPresetSavedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = vm) {
        vm.showPresetSavedEvent
            .consumeEach { showPresetSavedDialog = true }
            .collect()
    }

    RecordWidgetContent(
        vm = vm,
        navigateUp = vm.navController::navigateUp,
        onDone = vm::addWidgetFromSettings,
    )

    if (showPresetSavedDialog) {
        val onDismiss = {
            showPresetSavedDialog = false
            vm.trackPresetSaved()
            vm.navController.navigateUp()
        }
        ConfirmDialog(
            message = stringResource(Res.string.record_widget_preset_saved, stringResource(Res.string.app_name)),
            confirmText = stringResource(Res.string.cta_complete),
            onConfirm = onDismiss,
            onDismiss = onDismiss,
        )
    }
}

/**
 * The content is shared by the settings entry and the Android widget configuration activity.
 */
@Composable
internal fun RecordWidgetContent(
    vm: RecordWidgetViewModel,
    navigateUp: () -> Unit,
    onDone: () -> Unit,
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val selectedItems by vm.selectedItems.collectAsStateWithLifecycle()
    val isDoneEnabled by vm.isDoneEnabled.collectAsStateWithLifecycle()

    RecordWidgetContent(
        bookName = vm.book?.name.orEmpty(),
        uiState = uiState,
        selectedItems = selectedItems,
        isDoneEnabled = isDoneEnabled,
        navigateUp = navigateUp,
        onItemClick = vm::toggleItem,
        onDone = onDone,
    )
}

@Composable
private fun RecordWidgetContent(
    bookName: String,
    uiState: RecordWidgetUiState,
    selectedItems: List<RecordWidgetItem>,
    isDoneEnabled: Boolean,
    navigateUp: () -> Unit,
    onItemClick: (RecordWidgetItem) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.light)
    ) {
        TopBar(
            title = stringResource(Res.string.record_widget_title),
            navigateUp = navigateUp,
            menuActions = {
                MenuAction(
                    imageVector = vectorResource(Res.drawable.ic_check),
                    description = stringResource(Res.string.cta_complete),
                    enabled = isDoneEnabled,
                    onClick = onDone,
                    modifier = Modifier.thenIf(UiTestFlags.enabled) {
                        Modifier.semantics { contentDescription = "record_widget_done" }
                    }
                )
            }
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(Res.string.record_widget_book, bookName),
                fontSize = FontSize.SemiLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.record_widget_hint, RECORD_WIDGET_MAX_ITEMS),
                fontSize = FontSize.Small,
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
        ) {
            when (uiState) {
                RecordWidgetUiState.Loading -> InfiniteCircularProgress()
                is RecordWidgetUiState.Loaded -> if (uiState.items.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.record_widget_empty),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(
                            items = uiState.items,
                            key = { _, frequentItem ->
                                frequentItem.item.let { "${it.type}_${it.category}_${it.name}" }
                            }
                        ) { index, frequentItem ->
                            FrequentItemRow(
                                frequentItem = frequentItem,
                                isSelected = frequentItem.item in selectedItems,
                                isLast = index == uiState.items.lastIndex,
                                onClick = { onItemClick(frequentItem.item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequentItemRow(
    frequentItem: FrequentItem,
    isSelected: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val item = frequentItem.item
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .thenIf(UiTestFlags.enabled) {
                Modifier.semantics {
                    contentDescription = "record_widget_item_${item.type}_${item.category}_${item.name}"
                }
            }
            .rippleClick(color = LocalAppColors.current.dark, onClick = onClick)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Icon(
                imageVector = vectorResource(
                    if (isSelected) Res.drawable.ic_check_circle else Res.drawable.ic_check_circle_outline
                ),
                tint = LocalAppColors.current.dark,
                modifier = Modifier.padding(end = 16.dp),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1F),
            ) {
                Text(
                    text = item.name,
                    fontSize = FontSize.SemiLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillLabel(text = item.category)
                    if (item.type == RecordType.Income) {
                        PillLabel(text = stringResource(Res.string.record_income))
                    }
                }
            }

            // How many times the item was recorded in the last 30 days.
            Text(
                text = pluralStringResource(Res.plurals.record_widget_times, frequentItem.count, frequentItem.count),
                fontSize = FontSize.SemiLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        if (!isLast) {
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

@Composable
private fun PillLabel(text: String) {
    Text(
        text = text,
        fontSize = FontSize.Small,
        color = LocalAppColors.current.light,
        singleLine = true,
        modifier = Modifier
            .background(
                color = LocalAppColors.current.primary,
                shape = CircleShape
            )
            .padding(vertical = 1.dp, horizontal = 8.dp)
    )
}

@Preview
@Composable
private fun RecordWidgetContent_Preview() = AppTheme {
    val items = listOf(
        FrequentItem(RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Dinner"), count = 12),
        FrequentItem(RecordWidgetItem(type = RecordType.Expense, category = "Transport", name = "Transport"), count = 8),
        FrequentItem(RecordWidgetItem(type = RecordType.Income, category = "Salary", name = "Salary"), count = 1),
    )
    RecordWidgetContent(
        bookName = "Family Book",
        uiState = RecordWidgetUiState.Loaded(items),
        selectedItems = items.take(1).map { it.item },
        isDoneEnabled = true,
        navigateUp = {},
        onItemClick = {},
        onDone = {},
    )
}
