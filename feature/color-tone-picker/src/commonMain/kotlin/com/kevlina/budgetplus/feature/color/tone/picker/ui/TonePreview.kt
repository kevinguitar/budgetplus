package com.kevlina.budgetplus.feature.color.tone.picker.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.color_tone_preview_title
import budgetplus.core.common.generated.resources.cta_invite
import budgetplus.core.common.generated.resources.default_expense_categories
import budgetplus.core.common.generated.resources.ic_group_add
import budgetplus.core.common.generated.resources.ic_settings
import budgetplus.core.common.generated.resources.record_note
import budgetplus.core.common.generated.resources.record_note_placeholder_expense
import budgetplus.core.common.generated.resources.settings_description
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.getCurrencySymbol
import com.kevlina.budgetplus.core.common.getDefaultCurrencyCode
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.MenuAction
import com.kevlina.budgetplus.core.ui.RecordTypeTab
import com.kevlina.budgetplus.core.ui.SingleDatePicker
import com.kevlina.budgetplus.core.ui.TOP_BAR_DARKEN_FACTOR
import com.kevlina.budgetplus.core.ui.TextField
import com.kevlina.budgetplus.core.ui.TopBar
import com.kevlina.budgetplus.core.ui.darken
import com.kevlina.budgetplus.feature.category.pills.CategoriesGrid
import com.kevlina.budgetplus.feature.category.pills.CategoriesGridState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun TonePreview(
    modifier: Modifier = Modifier,
) {

    val expenseCategoriesArray = stringArrayResource(Res.array.default_expense_categories)
    val expenseCategories = remember { expenseCategoriesArray.toList() }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .clip(AppTheme.cardShape)
            .border(
                width = 1.dp,
                color = LocalAppColors.current.primary.darken(TOP_BAR_DARKEN_FACTOR),
                shape = RoundedCornerShape(ColorToneConstants.CardCornerRadius)
            )
    ) {

        TopBar(
            title = stringResource(Res.string.color_tone_preview_title),
            menuActions = {
                MenuAction(
                    imageVector = vectorResource(Res.drawable.ic_group_add),
                    description = stringResource(Res.string.cta_invite),
                )

                MenuAction(
                    imageVector = vectorResource(Res.drawable.ic_settings),
                    description = stringResource(Res.string.settings_description),
                )
            },
            applyStatusBarPadding = false
        )

        RecordTypeTab(
            selected = RecordType.Expense,
            onTypeSelected = { },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        CategoriesGrid(
            state = CategoriesGridState(
                expenseCategories = MutableStateFlow(expenseCategories),
                incomeCategories = MutableStateFlow(emptyList()),
                type = MutableStateFlow(RecordType.Expense),
                selectedCategory = MutableStateFlow(expenseCategories.first()),
                onCategorySelected = {},
                cardPaddingValues = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        TextField(
            state = rememberTextFieldState(""),
            title = stringResource(Res.string.record_note),
            placeholder = stringResource(Res.string.record_note_placeholder_expense),
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {

            SingleDatePicker(
                date = LocalDate.now(),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            TextField(
                state = rememberTextFieldState("1,680"),
                fontSize = FontSize.Header,
                letterSpacing = 0.5.sp,
                enabled = false,
                title = getCurrencySymbol(getDefaultCurrencyCode()),
                modifier = Modifier.weight(1F)
            )
        }
    }
}

@Preview
@Composable
private fun TonePreview_Preview() = AppTheme {
    TonePreview()
}