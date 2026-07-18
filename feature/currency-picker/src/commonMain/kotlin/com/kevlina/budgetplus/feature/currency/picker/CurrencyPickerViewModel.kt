package com.kevlina.budgetplus.feature.currency.picker

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
import com.kevlina.budgetplus.core.data.local.Preference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@AssistedInject
class CurrencyPickerViewModel(
    @Assisted private val params: BookDest.CurrencyPicker,
    private val navController: NavController<BookDest>,
    private val bookRepo: BookRepo,
    private val currencyExchangeRepo: CurrencyExchangeRepo,
    private val preference: Preference,
    private val snackbarSender: SnackbarSender,
) : ViewModel() {

    val keyword = TextFieldState()

    val title: StringResource
        get() = when (params.purpose) {
            Purpose.Book -> Res.string.currency_picker_book_title
            Purpose.Preferred -> Res.string.currency_picker_preferred_title
        }

    private val pinnedCurrenciesKey = stringSetPreferencesKey("pinnedCurrencies")
    private val pinnedCurrencies = preference.of(pinnedCurrenciesKey)

    private val hasShownCurrencyDisclaimerKey = booleanPreferencesKey("hasShownCurrencyDisclaimerCache")

    private val currentCurrencyCode = when (params.purpose) {
        Purpose.Book -> bookRepo.bookState.value?.currencyCode
        Purpose.Preferred -> currencyExchangeRepo.preferredCurrencyCode
    }

    private val defaultCurrencyCode = getDefaultCurrencyCode()
    private val availableCurrencies = getAvailableCurrencies()

    val currencies: StateFlow<List<CurrencyState>?> = combine(
        snapshotFlow { keyword.text },
        pinnedCurrencies
    ) { keyword, pinned ->
        if (keyword.isBlank()) {
            availableCurrencies
        } else {
            availableCurrencies.filter {
                it.name.contains(keyword, ignoreCase = true) ||
                    it.currencyCode.contains(keyword, ignoreCase = true)
            }
        }
            .map { currency ->
                CurrencyState(
                    currency = currency,
                    isSelected = currentCurrencyCode == currency.currencyCode,
                    isPinned = currency.currencyCode in pinned.orEmpty(),
                )
            }
            // Sort by symbol, so it looks nicer from the beginning
            .sortedByDescending { it.currency.symbol }
            // Then place the default currency at the front
            .sortedByDescending { it.currency.currencyCode == defaultCurrencyCode }
            // Then place the pinned currencies at the front
            .sortedByDescending { it.isPinned }
            // Then place the selected one (if exists) at the front
            .sortedByDescending { it.isSelected }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    suspend fun hasShownCurrencyDisclaimer(): Boolean {
        val hasShown = preference.of(hasShownCurrencyDisclaimerKey).first() == true
        if (!hasShown) {
            preference.update(hasShownCurrencyDisclaimerKey, true)
        }
        return hasShown
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

    suspend fun onCurrencyPinned(currency: Currency) {
        val pinnedCurrencies = pinnedCurrencies.first().orEmpty()
        val isPinned = currency.currencyCode in pinnedCurrencies
        preference.update(
            key = pinnedCurrenciesKey,
            value = if (isPinned) {
                pinnedCurrencies - currency.currencyCode
            } else {
                pinnedCurrencies + currency.currencyCode
            }
        )
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