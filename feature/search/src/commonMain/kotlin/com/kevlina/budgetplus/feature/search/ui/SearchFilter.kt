package com.kevlina.budgetplus.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.overview_author_anonymous
import budgetplus.core.common.generated.resources.overview_author_everyone
import budgetplus.core.common.generated.resources.record_expense
import budgetplus.core.common.generated.resources.record_income
import budgetplus.core.common.generated.resources.search_all_categories
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppDialog
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.DateRangePickerDialog
import com.kevlina.budgetplus.core.ui.DropdownItem
import com.kevlina.budgetplus.core.ui.DropdownMenu
import com.kevlina.budgetplus.core.ui.ModalBottomSheet
import com.kevlina.budgetplus.feature.category.pills.CategoriesGrid
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SearchFilter(
    modifier: Modifier = Modifier,
    state: SearchFilterState,
) {
    val type by state.type.collectAsStateWithLifecycle()
    val category by state.category.collectAsStateWithLifecycle()
    val period = state.period.collectAsStateWithLifecycle().value
    val author by state.author.collectAsStateWithLifecycle()
    val allAuthors by state.allAuthors.collectAsStateWithLifecycle()
    val showAuthorSelector = allAuthors.size > 1

    var isTypePickerShown by rememberSaveable { mutableStateOf(false) }
    var isCategoryGridShown by rememberSaveable { mutableStateOf(false) }
    var isPeriodPickerShown by rememberSaveable { mutableStateOf(false) }
    var isCustomDateRangePickerShown by rememberSaveable { mutableStateOf(false) }
    var isAuthorPickerShown by rememberSaveable { mutableStateOf(false) }

    // Dismiss category grid when category is selected
    LaunchedEffect(category) {
        isCategoryGridShown = false
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Box {
            FilterPill(
                state = FilterPillState(
                    placeholder = null,
                    selection = stringResource(
                        when (type) {
                            RecordType.Expense -> Res.string.record_expense
                            RecordType.Income -> Res.string.record_income
                        }
                    ),
                    onClick = { isTypePickerShown = true }
                )
            )

            DropdownMenu(
                expanded = isTypePickerShown,
                onDismissRequest = { isTypePickerShown = false }
            ) {
                DropdownItem(name = stringResource(Res.string.record_expense)) {
                    isTypePickerShown = false
                    state.selectType(RecordType.Expense)
                }

                DropdownItem(name = stringResource(Res.string.record_income)) {
                    isTypePickerShown = false
                    state.selectType(RecordType.Income)
                }
            }
        }

        FilterPill(
            state = FilterPillState(
                placeholder = stringResource(Res.string.search_all_categories),
                selection = category.name,
                onClick = { isCategoryGridShown = true }
            )
        )

        FilterPill(
            state = FilterPillState(
                placeholder = null,
                selection = period.text(),
                onClick = { isPeriodPickerShown = true }
            )
        )

        if (showAuthorSelector) {
            Box {
                FilterPill(
                    state = FilterPillState(
                        placeholder = stringResource(Res.string.overview_author_everyone),
                        selection = author?.name,
                        onClick = { isAuthorPickerShown = true }
                    )
                )

                DropdownMenu(
                    expanded = isAuthorPickerShown,
                    onDismissRequest = { isAuthorPickerShown = false }
                ) {
                    DropdownItem(name = stringResource(Res.string.overview_author_everyone)) {
                        isAuthorPickerShown = false
                        state.selectAuthor(null)
                    }

                    allAuthors.forEach { author ->
                        DropdownItem(name = author.name ?: stringResource(Res.string.overview_author_anonymous)) {
                            isAuthorPickerShown = false
                            state.selectAuthor(author)
                        }
                    }
                }
            }
        }
    }

    if (isCategoryGridShown) {
        AppDialog(onDismissRequest = { isCategoryGridShown = false }) {
            CategoriesGrid(state = state.categoryGrid)
        }
    }

    if (isPeriodPickerShown) {
        ModalBottomSheet(
            onDismissRequest = { isPeriodPickerShown = false }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SearchPeriodCell(
                    period = SearchPeriod.PastMonth,
                    isSelected = period == SearchPeriod.PastMonth,
                    onClick = {
                        isPeriodPickerShown = false
                        state.selectPeriod(SearchPeriod.PastMonth)
                    }
                )
                SearchPeriodCell(
                    period = SearchPeriod.PastHalfYear,
                    isSelected = period == SearchPeriod.PastHalfYear,
                    onClick = {
                        isPeriodPickerShown = false
                        state.selectPeriod(SearchPeriod.PastHalfYear)
                    }
                )
                SearchPeriodCell(
                    period = SearchPeriod.PastYear,
                    isSelected = period == SearchPeriod.PastYear,
                    onClick = {
                        isPeriodPickerShown = false
                        state.selectPeriod(SearchPeriod.PastYear)
                    }
                )
                SearchPeriodCell(
                    period = (period as? SearchPeriod.Custom) ?: SearchPeriod.Custom.Unselected,
                    isSelected = period is SearchPeriod.Custom,
                    onClick = { isCustomDateRangePickerShown = true }
                )
            }
        }
    }

    if (isCustomDateRangePickerShown) {
        DateRangePickerDialog(
            startDate = (period as? SearchPeriod.Custom)?.from,
            endDate = (period as? SearchPeriod.Custom)?.until,
            onDismiss = { isCustomDateRangePickerShown = false },
            onRangePicked = { from, until ->
                isCustomDateRangePickerShown = false
                isPeriodPickerShown = false
                state.selectPeriod(SearchPeriod.Custom(from, until))
            }
        )
    }
}

@Preview
@Composable
private fun SearchFilter_Preview() = AppTheme(ThemeColors.Dusk) {
    SearchFilter(
        state = SearchFilterState.preview,
        modifier = Modifier
            .background(LocalAppColors.current.light)
            .padding(vertical = 16.dp)
    )
}