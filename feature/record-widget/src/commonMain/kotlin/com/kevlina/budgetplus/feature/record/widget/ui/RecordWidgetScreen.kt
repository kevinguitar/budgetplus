package com.kevlina.budgetplus.feature.record.widget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.app_name
import budgetplus.core.common.generated.resources.book_selection
import budgetplus.core.common.generated.resources.cta_complete
import budgetplus.core.common.generated.resources.ic_arrow_drop_down
import budgetplus.core.common.generated.resources.ic_check
import budgetplus.core.common.generated.resources.ic_check_circle_outline
import budgetplus.core.common.generated.resources.record_income
import budgetplus.core.common.generated.resources.record_widget_book
import budgetplus.core.common.generated.resources.record_widget_dynamic_colors
import budgetplus.core.common.generated.resources.record_widget_dynamic_colors_desc
import budgetplus.core.common.generated.resources.record_widget_empty
import budgetplus.core.common.generated.resources.record_widget_hint
import budgetplus.core.common.generated.resources.record_widget_position
import budgetplus.core.common.generated.resources.record_widget_preset_saved
import budgetplus.core.common.generated.resources.record_widget_times
import budgetplus.core.common.generated.resources.record_widget_title
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.UiTestFlags
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.ConfirmDialog
import com.kevlina.budgetplus.core.ui.DropdownItemModel
import com.kevlina.budgetplus.core.ui.DropdownMenu
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.InfiniteCircularProgress
import com.kevlina.budgetplus.core.ui.MenuAction
import com.kevlina.budgetplus.core.ui.Switch
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
        vm.start(widgetId = null)
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
 *
 * @param applyNavBarPadding Keep the content above the navigation bar. Only needed when the
 *  screen is rendered directly by an edge to edge activity, i.e. the widget configuration activity.
 */
@Composable
internal fun RecordWidgetContent(
    vm: RecordWidgetViewModel,
    navigateUp: () -> Unit,
    onDone: () -> Unit,
    applyNavBarPadding: Boolean = false,
) {
    val book by vm.book.collectAsStateWithLifecycle()
    val books by vm.books.collectAsStateWithLifecycle()
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val selectedItems by vm.selectedItems.collectAsStateWithLifecycle()
    val isDoneEnabled by vm.isDoneEnabled.collectAsStateWithLifecycle()
    val useDynamicColors by vm.useDynamicColors.collectAsStateWithLifecycle()

    RecordWidgetContent(
        book = book,
        books = books,
        uiState = uiState,
        selectedItems = selectedItems,
        isDoneEnabled = isDoneEnabled,
        useDynamicColors = useDynamicColors.takeIf { vm.isDynamicColorSupported },
        navigateUp = navigateUp,
        onBookSelect = vm::selectBook,
        onDynamicColorsChange = vm::setUseDynamicColors,
        onItemClick = vm::toggleItem,
        onDone = onDone,
        applyNavBarPadding = applyNavBarPadding,
    )
}

@Composable
private fun RecordWidgetContent(
    book: Book?,
    books: List<Book>,
    uiState: RecordWidgetUiState,
    selectedItems: List<RecordWidgetItem>,
    isDoneEnabled: Boolean,
    useDynamicColors: Boolean?,
    navigateUp: () -> Unit,
    onBookSelect: (Book) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onItemClick: (RecordWidgetItem) -> Unit,
    onDone: () -> Unit,
    applyNavBarPadding: Boolean = false,
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

        val previewColors = (if (useDynamicColors == true) rememberDynamicWidgetPreviewColors() else null)
            ?: WidgetPreviewColors.fromTheme(LocalAppColors.current)

        // The header, the toggle and the preview scroll with the items, so the list keeps enough
        // room on small screens.
        LazyColumn(
            // Scroll behind the navigation bar, but the last item can scroll above it.
            contentPadding = if (applyNavBarPadding) {
                WindowInsets.navigationBars.asPaddingValues()
            } else {
                PaddingValues()
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F),
        ) {
            item(key = "header") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BookHeader(book = book, books = books, onBookSelect = onBookSelect)
                    Text(
                        text = stringResource(Res.string.record_widget_hint, RECORD_WIDGET_MAX_ITEMS),
                        fontSize = FontSize.Small,
                    )
                }
            }

            if (useDynamicColors != null) {
                item(key = "dynamic_colors") {
                    DynamicColorsToggle(checked = useDynamicColors, onCheckedChange = onDynamicColorsChange)
                }
            }

            item(key = "preview") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    RecordWidgetPreview(
                        bookName = book?.name.orEmpty(),
                        items = selectedItems,
                        colors = previewColors,
                    )
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(color = LocalAppColors.current.primary)
                )
            }

            when (uiState) {
                RecordWidgetUiState.Loading -> item(key = "loading") {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        InfiniteCircularProgress()
                    }
                }

                is RecordWidgetUiState.Loaded -> if (uiState.items.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(Res.string.record_widget_empty),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 32.dp),
                        )
                    }
                } else {
                    itemsIndexed(
                        items = uiState.items,
                        key = { _, frequentItem ->
                            frequentItem.item.let { "${it.type}_${it.category}_${it.name}" }
                        }
                    ) { index, frequentItem ->
                        val position = selectedItems.indexOf(frequentItem.item)
                        FrequentItemRow(
                            frequentItem = frequentItem,
                            position = if (position >= 0) position + 1 else null,
                            isLast = index == uiState.items.lastIndex,
                            onClick = { onItemClick(frequentItem.item) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Android 12+: color the widget by Material You instead of the app's theme.
 */
@Composable
private fun DynamicColorsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val title = stringResource(Res.string.record_widget_dynamic_colors)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .rippleClick(color = LocalAppColors.current.dark, onClick = { onCheckedChange(!checked) })
            .thenIf(UiTestFlags.enabled) {
                Modifier.semantics {
                    contentDescription = "record_widget_dynamic_colors_${if (checked) "on" else "off"}"
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1F),
        ) {
            Text(
                text = title,
                fontSize = FontSize.SemiLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(Res.string.record_widget_dynamic_colors_desc),
                fontSize = FontSize.Small,
            )
        }

        // The switch is a separate accessibility node from the row, label it for the screen readers.
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

/**
 * "Adds to: {book}", tap to tie the widget to another book when the user has more than one.
 */
@Composable
private fun BookHeader(
    book: Book?,
    books: List<Book>,
    onBookSelect: (Book) -> Unit,
) {
    var isMenuShown by remember { mutableStateOf(false) }
    val canSwitch = books.size > 1

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.thenIf(canSwitch) {
                Modifier.rippleClick(color = LocalAppColors.current.dark, onClick = { isMenuShown = true })
            }
        ) {
            Text(
                text = stringResource(Res.string.record_widget_book, book?.name.orEmpty()),
                fontSize = FontSize.SemiLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1F, fill = false),
            )
            if (canSwitch) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_arrow_drop_down),
                    contentDescription = stringResource(Res.string.book_selection),
                    tint = LocalAppColors.current.dark,
                )
            }
        }

        DropdownMenu(
            expanded = isMenuShown,
            onDismissRequest = { isMenuShown = false },
            items = books.map { option ->
                val isSelected = option.id == book?.id
                DropdownItemModel(
                    name = option.name,
                    trailingIcon = if (isSelected) vectorResource(Res.drawable.ic_check) else null,
                    iosSfSymbol = if (isSelected) "checkmark" else null,
                    onClick = {
                        onBookSelect(option)
                        isMenuShown = false
                    }
                )
            },
            offset = DpOffset(0.dp, 4.dp),
            modifier = Modifier.heightIn(max = 300.dp),
        )
    }
}

/**
 * @param position The 1-based position of the item on the widget, null if it's not selected.
 */
@Composable
private fun FrequentItemRow(
    frequentItem: FrequentItem,
    position: Int?,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val item = frequentItem.item
    val isSelected = position != null
    val positionDescription = position?.let { stringResource(Res.string.record_widget_position, it) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .thenIf(UiTestFlags.enabled) {
                Modifier.semantics {
                    contentDescription = "record_widget_item_${item.type}_${item.category}_${item.name}"
                }
            }
            .rippleClick(color = LocalAppColors.current.dark, onClick = onClick)
            // The badge is decorative, expose the selection and the position to the screen readers.
            .semantics {
                role = Role.Checkbox
                toggleableState = ToggleableState(isSelected)
                if (positionDescription != null) stateDescription = positionDescription
            }
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(end = 16.dp),
            ) {
                if (position == null) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_check_circle_outline),
                        tint = LocalAppColors.current.dark,
                    )
                } else {
                    PositionBadge(position = position)
                }
            }

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

/**
 * The order of the item on the widget, in place of the check icon. It's the same size as the
 * outline icon so the row doesn't shift when it's selected.
 */
@Composable
private fun PositionBadge(position: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            // The visible circle of the outline icon is 20dp in a 24dp box.
            .padding(2.dp)
            .size(20.dp)
            .background(color = LocalAppColors.current.dark, shape = CircleShape)
    ) {
        Text(
            text = position.toString(),
            fontSize = FontSize.Small,
            fontWeight = FontWeight.Bold,
            color = LocalAppColors.current.light,
            singleLine = true,
        )
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
        book = Book(id = "family", name = "Family Book"),
        books = listOf(Book(id = "family", name = "Family Book"), Book(id = "travel", name = "Travel")),
        uiState = RecordWidgetUiState.Loaded(items),
        selectedItems = listOf(items[2].item, items[0].item),
        isDoneEnabled = true,
        useDynamicColors = false,
        navigateUp = {},
        onBookSelect = {},
        onDynamicColorsChange = {},
        onItemClick = {},
        onDone = {},
    )
}
