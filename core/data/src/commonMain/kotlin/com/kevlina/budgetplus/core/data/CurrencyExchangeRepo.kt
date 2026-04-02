package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.common.Currency
import kotlinx.coroutines.flow.Flow

interface CurrencyExchangeRepo {

    val exchangeRateChange: Flow<Unit>

    /**
     * @return The currency code that the user prefers to use.
     */
    suspend fun getPreferredCurrencyCode(): String

    /**
     * Updates the currency that the user prefers to use.
     */
    suspend fun updatePreferredCurrency(currency: Currency)

    /**
     * Formats the given price using the preferred currency.
     * @return Formatted price string, or null if the preferred currency rate is not resolved.
     */
    suspend fun formatPreferredCurrency(price: Double): String?
}