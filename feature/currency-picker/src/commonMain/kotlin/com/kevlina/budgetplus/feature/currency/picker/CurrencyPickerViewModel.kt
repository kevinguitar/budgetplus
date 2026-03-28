package com.kevlina.budgetplus.feature.currency.picker

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.currency_picker_edit_success
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.getAvailableCurrencies
import com.kevlina.budgetplus.core.common.getDefaultCurrencyCode
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.local.Preference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.resources.getString

@ViewModelKey
@ContributesIntoMap(AppScope::class)
class CurrencyPickerViewModel(
    val navController: NavController<BookDest>,
    private val bookRepo: BookRepo,
    private val snackbarSender: SnackbarSender,
    private val preference: Preference,
) : ViewModel() {

    val keyword = TextFieldState()

    private val hasShownCurrencyDisclaimerKey = booleanPreferencesKey("hasShownCurrencyDisclaimerCache")
    private val currentCurrencyCode = bookRepo.bookState.value?.currencyCode

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

    suspend fun hasShownCurrencyDisclaimer(): Boolean {
        val hasShown = preference.of(hasShownCurrencyDisclaimerKey).first() == true
        if (!hasShown) {
            preference.update(hasShownCurrencyDisclaimerKey, true)
        }
        return hasShown
    }

    private fun onSearch(keyword: CharSequence) {
        currencies.value = allCurrencies.filter {
            it.currency.name.contains(keyword, ignoreCase = true) ||
                it.currency.currencyCode.contains(keyword, ignoreCase = true)
        }
    }

    suspend fun onCurrencyPicked(state: CurrencyState) {
        val bookName = bookRepo.bookState.value?.name ?: return
        try {
            bookRepo.updateCurrency(state.currency.currencyCode)
            snackbarSender.send(getString(Res.string.currency_picker_edit_success, bookName, state.currency.name))
        } catch (e: Exception) {
            snackbarSender.sendError(e)
        }
        navController.navigateUp()
    }
}