package com.kevlina.budgetplus.feature.currency.picker

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.currency_picker_book_edit_success
import budgetplus.core.common.generated.resources.currency_picker_book_title
import budgetplus.core.common.generated.resources.currency_picker_preferred_edit_success
import budgetplus.core.common.generated.resources.currency_picker_preferred_title
import com.kevlina.budgetplus.core.common.Currency
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.getAvailableCurrencies
import com.kevlina.budgetplus.core.common.getDefaultCurrencyCode
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BookDest.CurrencyPicker.Purpose
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.CurrencyExchangeRepo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@AssistedInject
class CurrencyPickerViewModel(
    @Assisted private val params: BookDest.CurrencyPicker,
    private val navController: NavController<BookDest>,
    private val bookRepo: BookRepo,
    private val currencyExchangeRepo: CurrencyExchangeRepo,
    private val snackbarSender: SnackbarSender,
) : ViewModel() {

    val keyword = TextFieldState()

    val title: StringResource
        get() = when (params.purpose) {
            Purpose.Book -> Res.string.currency_picker_book_title
            Purpose.Preferred -> Res.string.currency_picker_preferred_title
        }

    private val currentCurrencyCode = when (params.purpose) {
        Purpose.Book -> bookRepo.bookState.value?.currencyCode
        Purpose.Preferred -> currencyExchangeRepo.preferredCurrencyCode
    }

    private val defaultCurrencyCode = getDefaultCurrencyCode()

    private val allCurrencies = getAvailableCurrencies()
        .map { currency ->
            CurrencyState(
                currency = currency,
                isSelected = currentCurrencyCode == currency.currencyCode
            )
        }
        // Sort by symbol, so it looks nicer from the beginning
        .sortedByDescending { it.currency.symbol }
        // Then place the default currency at the front
        .sortedByDescending { it.currency.currencyCode == defaultCurrencyCode }
        // Then place the selected one (if exists) at the front
        .sortedByDescending { it.isSelected }

    val currencies: StateFlow<List<CurrencyState>>
        field = MutableStateFlow(allCurrencies)

    init {
        snapshotFlow { keyword.text }
            .onEach(::onSearch)
            .launchIn(viewModelScope)
    }

    private fun onSearch(keyword: CharSequence) {
        currencies.value = allCurrencies.filter {
            it.currency.name.contains(keyword, ignoreCase = true) ||
                it.currency.currencyCode.contains(keyword, ignoreCase = true)
        }
    }

    suspend fun onCurrencyPicked(currency: Currency) {
        when (params.purpose) {
            Purpose.Book -> {
                val bookName = bookRepo.bookState.value?.name ?: return
                try {
                    bookRepo.updateCurrency(currency.currencyCode)
                    snackbarSender.send(getString(Res.string.currency_picker_book_edit_success, bookName, currency.name))
                } catch (e: Exception) {
                    snackbarSender.sendError(e)
                }
            }

            Purpose.Preferred -> {
                currencyExchangeRepo.updatePreferredCurrency(currency)
                snackbarSender.send(getString(Res.string.currency_picker_preferred_edit_success, currency.name))
            }
        }
        navigateUp()
    }

    fun navigateUp() {
        navController.navigateUp()
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(params: BookDest.CurrencyPicker): CurrencyPickerViewModel
    }
}