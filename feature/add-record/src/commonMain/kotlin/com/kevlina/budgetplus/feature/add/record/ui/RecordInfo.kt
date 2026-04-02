package com.kevlina.budgetplus.feature.add.record.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.record_note
import budgetplus.core.common.generated.resources.record_note_placeholder_expense
import budgetplus.core.common.generated.resources.record_note_placeholder_income
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.RecordTypeTab
import com.kevlina.budgetplus.core.ui.TextField
import com.kevlina.budgetplus.core.ui.thenIf
import com.kevlina.budgetplus.feature.category.pills.CategoriesGrid
import com.kevlina.budgetplus.feature.category.pills.CategoriesGridState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RecordInfo(
    state: RecordInfoState,
    modifier: Modifier = Modifier,
) {
    val type by state.type.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .thenIf(state.scrollable) {
                Modifier.verticalScroll(scrollState)
            }
            .padding(bottom = 16.dp)
    ) {

        RecordTypeTab(
            selected = type,
            onTypeSelected = state.setType,
            modifier = Modifier.padding(top = 16.dp)
        )

        CategoriesGrid(
            state = state.categoriesGridState,
            modifier = Modifier.fillMaxWidth()
        )

        val focusManager = LocalFocusManager.current

        TextField(
            state = state.note,
            title = stringResource(Res.string.record_note),
            placeholder = stringResource(
                when (type) {
                    RecordType.Expense -> Res.string.record_note_placeholder_expense
                    RecordType.Income -> Res.string.record_note_placeholder_income
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            onDone = { focusManager.moveFocus(FocusDirection.Next) }
        )

        DateAndPricing(
            state = state.dateAndPricingState,
            scrollState = scrollState,
        )
    }
}

@Stable
internal data class RecordInfoState(
    val type: StateFlow<RecordType>,
    val note: TextFieldState,
    val setType: (RecordType) -> Unit,
    val scrollable: Boolean,
    val categoriesGridState: CategoriesGridState,
    val dateAndPricingState: DateAndPricingState,
) {
    companion object {
        val preview = RecordInfoState(
            type = MutableStateFlow(RecordType.Expense),
            note = TextFieldState("Some cool daily stuff"),
            setType = {},
            scrollable = true,
            categoriesGridState = CategoriesGridState.preview,
            dateAndPricingState = DateAndPricingState.preview
        )
    }
}

@Preview
@Composable
private fun RecordInfo_Preview() = AppTheme {
    RecordInfo(
        state = RecordInfoState.preview,
        modifier = Modifier
            .background(LocalAppColors.current.light)
            .padding(horizontal = 16.dp)
    )
}