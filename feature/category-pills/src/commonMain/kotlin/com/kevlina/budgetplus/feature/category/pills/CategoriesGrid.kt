package com.kevlina.budgetplus.feature.category.pills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.category_edit_title
import budgetplus.core.common.generated.resources.cta_add
import budgetplus.core.common.generated.resources.cta_edit
import budgetplus.core.common.generated.resources.ic_add
import budgetplus.core.common.generated.resources.ic_drive_file_rename_outline
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.rippleClick
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
fun CategoriesGrid(
    state: CategoriesGridState,
    modifier: Modifier = Modifier,
) {
    val type by state.type.collectAsStateWithLifecycle()
    val expenseCategories by state.expenseCategories.collectAsStateWithLifecycle()
    val incomeCategories by state.incomeCategories.collectAsStateWithLifecycle()
    val selectedCategory by state.selectedCategory.collectAsStateWithLifecycle()

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier = modifier
    ) {

        when (type) {
            RecordType.Expense -> expenseCategories.forEach { item ->
                CategoryCard(
                    category = item,
                    isSelected = selectedCategory == item,
                    paddingValues = state.cardPaddingValues
                ) {
                    state.onCategorySelected(item)
                }
            }

            RecordType.Income -> incomeCategories.forEach { item ->
                CategoryCard(
                    category = item,
                    isSelected = selectedCategory == item,
                    paddingValues = state.cardPaddingValues
                ) {
                    state.onCategorySelected(item)
                }
            }
        }

        if (state.onEditClicked != null) {
            CategoryActionButton(
                icon = vectorResource(Res.drawable.ic_drive_file_rename_outline),
                name = stringResource(Res.string.cta_edit),
                onClick = state.onEditClicked
            )
        }

        if (state.onAddClicked != null) {
            CategoryActionButton(
                icon = vectorResource(Res.drawable.ic_add),
                name = stringResource(Res.string.cta_add),
                onClick = state.onAddClicked
            )
        }
    }
}

private val cardPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)

@Composable
fun CategoryCard(
    category: String,
    isSelected: Boolean,
    paddingValues: PaddingValues = cardPadding,
    onClick: () -> Unit,
) {

    Box(
        modifier = Modifier
            .background(
                shape = AppTheme.cardShape,
                color = if (isSelected) {
                    LocalAppColors.current.dark
                } else {
                    LocalAppColors.current.primary
                }
            )
            .clip(AppTheme.cardShape)
            .rippleClick(onClick = onClick)
    ) {

        Text(
            text = category,
            singleLine = true,
            color = LocalAppColors.current.light,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun CategoryActionButton(
    icon: ImageVector,
    name: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                shape = AppTheme.cardShape,
                color = LocalAppColors.current.primary
            )
            .clip(AppTheme.cardShape)
            .rippleClick(onClick = onClick)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(cardPadding)
        ) {

            Icon(
                imageVector = icon,
                contentDescription = stringResource(Res.string.category_edit_title),
                tint = LocalAppColors.current.light,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = name,
                singleLine = true,
                color = LocalAppColors.current.light
            )
        }
    }
}

@Stable
class CategoriesGridState(
    val expenseCategories: StateFlow<List<String>>,
    val incomeCategories: StateFlow<List<String>>,
    val type: StateFlow<RecordType>,
    val selectedCategory: StateFlow<String?>,
    val onCategorySelected: (String) -> Unit,
    val onEditClicked: (() -> Unit)? = null,
    val onAddClicked: (() -> Unit)? = null,
    val cardPaddingValues: PaddingValues = cardPadding,
) {
    companion object {
        val preview = CategoriesGridState(
            type = MutableStateFlow(RecordType.Expense),
            expenseCategories = MutableStateFlow(listOf(
                "Food", "Daily", "Transport", "Entertainment", "Rent", "Mobile", "Utility", "Other"
            )),
            incomeCategories = MutableStateFlow(emptyList()),
            onCategorySelected = {},
            onEditClicked = {},
            selectedCategory = MutableStateFlow("Daily")
        )
    }
}

fun CategoriesViewModel.toState(
    type: StateFlow<RecordType>,
    selectedCategory: StateFlow<String?> = category,
    onCategorySelected: (String) -> Unit = ::setCategory,
    onEditClicked: (() -> Unit)? = null,
    onAddClicked: (() -> Unit)? = null,
    cardPaddingValues: PaddingValues = cardPadding,
) = CategoriesGridState(
    expenseCategories = expenseCategories,
    incomeCategories = incomeCategories,
    type = type,
    selectedCategory = selectedCategory,
    onCategorySelected = onCategorySelected,
    onEditClicked = onEditClicked,
    onAddClicked = onAddClicked,
    cardPaddingValues = cardPaddingValues
)

@Preview
@Composable
private fun CategoryGrid_Preview() = AppTheme {
    CategoriesGrid(
        CategoriesGridState.preview,
        modifier = Modifier
            .background(LocalAppColors.current.light)
            .padding(all = 16.dp)
    )
}