package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.VisibleForTesting
import com.kevlina.budgetplus.core.common.Currency
import com.kevlina.budgetplus.core.data.CurrencyExchangeRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

@VisibleForTesting
class FakeCurrencyExchangeRepo(
    var preferredCurrencyCode: String = "USD"
) : CurrencyExchangeRepo {

    override val exchangeRateChange: Flow<Unit> = MutableSharedFlow()

    override suspend fun getPreferredCurrencyCode(): String = preferredCurrencyCode

    override suspend fun updatePreferredCurrency(currency: Currency) {
        preferredCurrencyCode = currency.currencyCode
    }

    override suspend fun formatPreferredCurrency(price: Double): String {
        return "$price $preferredCurrencyCode"
    }
}
