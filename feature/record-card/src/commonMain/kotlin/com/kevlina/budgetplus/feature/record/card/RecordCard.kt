package com.kevlina.budgetplus.feature.record.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.cta_delete
import budgetplus.core.common.generated.resources.cta_duplicate
import budgetplus.core.common.generated.resources.cta_select
import budgetplus.core.common.generated.resources.ic_check_circle
import budgetplus.core.common.generated.resources.ic_check_circle_outline
import budgetplus.core.common.generated.resources.ic_refresh
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.UiTestFlags
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.common.shortFormatted
import com.kevlina.budgetplus.core.data.remote.Author
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.isBatched
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.DropdownItem
import com.kevlina.budgetplus.core.ui.DropdownMenu
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.rippleClick
import com.kevlina.budgetplus.core.ui.thenIf
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

// UI-test-only stable target: each record cell exposes "record_cell_<index>" so tests
// can long-press/tap a specific row reliably (record titles can be duplicated).
private const val RECORD_CELL_DESC_PREFIX = "record_cell_"

@Composable
fun RecordCard(
    state: RecordCardState,
    modifier: Modifier = Modifier,
) {
    val item = state.item
    var isMenuShown by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .thenIf(UiTestFlags.enabled && state.index != null) {
                Modifier.semantics {
                    contentDescription = "$RECORD_CELL_DESC_PREFIX${state.index}"
                }
            }
            .rippleClick(
                color = LocalAppColors.current.dark,
                onClick = when {
                    // In selection mode, tapping toggles the selection immediately.
                    state.isSelectionMode -> state.onToggleSelect
                    state.canEdit -> state.onEdit
                    else -> {
                        {}
                    }
                },
                onLongClick = { isMenuShown = true }
            )
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            AnimatedVisibility(
                visible = state.isSelectionMode,
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
            ) {
                Icon(
                    imageVector = vectorResource(
                        if (state.isSelected) {
                            Res.drawable.ic_check_circle
                        } else {
                            Res.drawable.ic_check_circle_outline
                        }
                    ),
                    tint = LocalAppColors.current.dark,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1F),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    if (item.isBatched) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_refresh),
                            tint = LocalAppColors.current.primary,
                            size = 16.dp
                        )
                    }

                    Text(
                        text = item.name,
                        fontSize = FontSize.SemiLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = LocalDate.fromEpochDays(item.date).shortFormatted,
                    )

                    if (state.showCategory) {
                        PillLabel(text = item.category)
                    }

                    if (state.showAuthor) {
                        PillLabel(text = item.author?.name.orEmpty())
                    }
                }
            }

            Text(
                text = state.formattedPrice,
                fontSize = FontSize.SemiLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        if (!state.isLast) {
            Spacer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(color = LocalAppColors.current.primary)
            )
        }

        // Long click to display the menu
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.BottomEnd)
        ) {
            DropdownMenu(
                expanded = isMenuShown,
                onDismissRequest = { isMenuShown = false },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                DropdownItem(
                    name = stringResource(Res.string.cta_select),
                ) {
                    isMenuShown = false
                    state.onSelect()
                }

                DropdownItem(
                    name = stringResource(Res.string.cta_duplicate),
                ) {
                    isMenuShown = false
                    state.onDuplicate()
                }

                if (state.canEdit) {
                    DropdownItem(
                        name = stringResource(Res.string.cta_delete),
                    ) {
                        isMenuShown = false
                        state.onDelete()
                    }
                }
            }
        }
    }
}

@Immutable
data class RecordCardState(
    val item: Record,
    val formattedPrice: String,
    val isLast: Boolean,
    val canEdit: Boolean,
    val showCategory: Boolean,
    val showAuthor: Boolean,
    val isSelectionMode: Boolean = false,
    val isSelected: Boolean = false,
    /** Position of the card in its list; used only to expose a stable UI-test target. */
    val index: Int? = null,
    val onEdit: () -> Unit,
    val onDuplicate: () -> Unit,
    val onDelete: () -> Unit,
    val onSelect: () -> Unit = {},
    val onToggleSelect: () -> Unit = {},
) {
    companion object {
        val preview = RecordCardState(
            item = Record(
                type = RecordType.Income,
                date = LocalDate.now().toEpochDays(),
                category = "Food",
                name = "Fancy Restaurant",
                price = 453.93,
                author = Author(id = "", name = "Kevin")
            ),
            formattedPrice = "$453.93",
            isLast = false,
            canEdit = true,
            showCategory = true,
            showAuthor = true,
            onEdit = {},
            onDuplicate = {},
            onDelete = {}
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

@Preview(showBackground = true)
@Composable
private fun RecordCard_Preview() = AppTheme {
    RecordCard(state = RecordCardState.preview)
}