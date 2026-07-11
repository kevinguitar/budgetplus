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
     * Formats the given book's currency price.
     * @return Formatted price string, or null if the book's currency matches the preferred currency
     *  (i.e. no conversion is meaningful) or the book's currency is unknown.
     */
    fun formatBookCurrency(price: Double, alwaysShowSymbol: Boolean = false): String?

    /**
     * Converts a price expressed in [fromCurrencyCode] into the book's currency.
     *
     * @param price The amount expressed in [fromCurrencyCode].
     * @param fromCurrencyCode The currency code the [price] is expressed in, defaults to the
     *  preferred currency.
     * @return The converted amount in the book's currency, or null if the rate is not resolved.
     */
    fun convertToBookCurrency(
        price: Double,
        fromCurrencyCode: String = preferredCurrencyCode,
    ): Double?

    /**
     * Toggle whether to display prices in the preferred currency or book's currency.
     */
    fun toggleDisplayInPreferredCurrency()
}