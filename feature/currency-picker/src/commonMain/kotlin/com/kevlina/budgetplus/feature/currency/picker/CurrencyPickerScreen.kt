package com.kevlina.budgetplus.feature.currency.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.TopBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun CurrencyPickerScreen(
    vm: CurrencyPickerViewModel,
) {
    val currencies by vm.currencies.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.light)
    ) {

        TopBar(
            title = stringResource(vm.title),
            navigateUp = { vm.navigateUp() },
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
        ) {
            CurrencyPickerContent(
                keyword = vm.keyword,
                currencyStates = currencies,
                onCurrencyPicked = { currency ->
                    coroutineScope.launch {
                        vm.onCurrencyPicked(currency)
                    }
                }
            )
        }
    }
}