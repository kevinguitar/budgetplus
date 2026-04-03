package com.kevlina.budgetplus.core.common

import co.touchlab.kermit.Logger
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*
import java.util.Currency as JavaCurrency

actual fun getCurrencySymbol(currencyCode: String?): String {
    val code = currencyCode ?: fallbackCurrencyCode
    return try {
        JavaCurrency.getInstance(code).getSymbol(Locale.getDefault())
    } catch (e: Exception) {
        Logger.e(e) { "Failed to get currency symbol for code: $code" }
        code
    }
}

actual fun formatPriceWithCurrency(price: Double, currencyCode: String?, alwaysShowSymbol: Boolean): String {
    val currency = try {
        JavaCurrency.getInstance(currencyCode ?: fallbackCurrencyCode)
    } catch (_: Exception) {
        null
    }

    return if (currency != null && (alwaysShowSymbol || currency.getSymbol(Locale.getDefault()).length == 1)) {
        val pricingFormat = NumberFormat.getCurrencyInstance().apply {
            this.currency = currency
            roundingMode = RoundingMode.HALF_UP
            minimumFractionDigits = 0
            maximumFractionDigits = 2
        }
        pricingFormat.format(price)
    } else {
        price.plainPriceString
    }
}

actual fun getDefaultCurrencyCode(): String {
    return try {
        JavaCurrency.getInstance(Locale.getDefault()).currencyCode
    } catch (_: Exception) {
        // It fails on CI
        fallbackCurrencyCode
    }
}

actual fun getAvailableCurrencies(): List<Currency> {
    return JavaCurrency.getAvailableCurrencies().map {
        Currency(
            name = it.displayName,
            currencyCode = it.currencyCode,
            symbol = it.getSymbol(Locale.getDefault())
        )
    }
}