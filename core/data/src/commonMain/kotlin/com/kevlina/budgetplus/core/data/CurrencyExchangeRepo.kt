package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.common.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface CurrencyExchangeRepo {

    val exchangeRateChange: Flow<Unit>

    /**
     * @return The symbol for the preferred currency.
     */
    val preferredCurrencySymbol: Flow<String?>

    /**
     * @return The currency code that the user prefers to use.
     */
    val preferredCurrencyCode: String

    /**
     * @return Whether to display prices in the preferred currency or book's currency.
     */
    val displayInPreferredCurrency: StateFlow<Boolean>

    /**
     * Updates the currency that the user prefers to use.
     */
    fun updatePreferredCurrency(currency: Currency)

    /**
     * Formats the given price using the preferred currency.
     * @return Formatted price string, or null if the preferred currency rate is not resolved.
     */
    fun formatPreferredCurrency(price: Double, alwaysShowSymbol: Boolean = false): String?

    /**
     * Toggle whether to display prices in the preferred currency or book's currency.
     */
    fun toggleDisplayInPreferredCurrency()
}