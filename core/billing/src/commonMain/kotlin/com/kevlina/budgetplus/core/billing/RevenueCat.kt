package com.kevlina.budgetplus.core.billing

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure

expect val revenueCatApiKey: String

fun configureRevenueCat() {
    Purchases.logLevel = LogLevel.DEBUG
    Purchases.configure(apiKey = revenueCatApiKey)
}