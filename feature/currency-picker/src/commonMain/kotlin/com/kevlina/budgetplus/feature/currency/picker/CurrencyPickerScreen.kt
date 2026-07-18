package com.kevlina.budgetplus.feature.currency.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.currency_picker_no_conversion_disclaimer
import com.kevlina.budgetplus.core.common.Currency
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.ConfirmDialog
import com.kevlina.budgetplus.core.ui.TopBar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun CurrencyPickerScreen(
    vm: CurrencyPickerViewModel,
) {
    val currencies by vm.currencies.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var currencyDisclaimerDialogState by remember { mutableStateOf<Currency?>(null) }

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
                        if (vm.hasShownCurrencyDisclaimer()) {
                            vm.onCurrencyPicked(currency)
                        } else {
                            currencyDisclaimerDialogState = currency
                        }
                    }
                },
                onCurrencyPinned = { currency ->
                    coroutineScope.launch {
                        vm.onCurrencyPinned(currency)
                    }
                }
            )
        }
    }

    currencyDisclaimerDialogState?.let { currency ->
        ConfirmDialog(
            message = stringResource(Res.string.currency_picker_no_conversion_disclaimer),
            onConfirm = {
                coroutineScope.launch {
                    vm.onCurrencyPicked(currency)
                    currencyDisclaimerDialogState = null
                }
            },
            onDismiss = { currencyDisclaimerDialogState = null },
        )
    }
}