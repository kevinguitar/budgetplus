package com.kevlina.budgetplus.feature.currency.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.currency_picker_hint
import com.kevlina.budgetplus.core.common.Currency
import com.kevlina.budgetplus.core.common.getAvailableCurrencies
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.SearchField
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.containerPadding
import com.kevlina.budgetplus.core.ui.rippleClick
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CurrencyPickerContent(
    keyword: TextFieldState,
    currencyStates: List<CurrencyState>,
    onCurrencyPicked: (Currency) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        val focusManager = LocalFocusManager.current

        SearchField(
            keyword = keyword,
            hint = stringResource(Res.string.currency_picker_hint),
            onDone = { focusManager.clearFocus() },
            modifier = Modifier
                .containerPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 136.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = AppTheme.listContentPaddings(all = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                currencyStates.forEach { state ->
                    item(key = state.currency.symbol) {
                        CurrencyCard(
                            state = state,
                            onClick = { onCurrencyPicked(state.currency) }
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(
                        SEARCH_GRADIENT_START to LocalAppColors.current.light,
                        SEARCH_GRADIENT_END to Color.Transparent
                    ))
            )
        }
    }
}

private const val SEARCH_GRADIENT_START = 0.4F
private const val SEARCH_GRADIENT_END = 1F

@Composable
private fun CurrencyCard(
    state: CurrencyState,
    onClick: () -> Unit,
) {
    val isSelected = state.isSelected

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .height(120.dp)
            .clip(AppTheme.cardShape)
            .background(if (isSelected) {
                LocalAppColors.current.dark
            } else {
                LocalAppColors.current.lightBg
            })
            .rippleClick(onClick = onClick)
            .padding(8.dp)
    ) {
        val textColor = if (isSelected) {
            LocalAppColors.current.light
        } else {
            LocalAppColors.current.dark
        }

        Text(
            text = state.currency.symbol,
            fontSize = FontSize.HeaderXLarge,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )

        Text(
            text = state.currency.name,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@PreviewScreenSizes
@Composable
private fun CurrencyPickerContent_Preview() = AppTheme {
    CurrencyPickerContent(
        keyword = TextFieldState(),
        currencyStates = getAvailableCurrencies().mapIndexed { index, currency ->
            CurrencyState(
                currency = currency,
                isSelected = index == 0
            )
        },
        onCurrencyPicked = {},
        modifier = Modifier.background(LocalAppColors.current.light)
    )
}
