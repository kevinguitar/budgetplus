package com.kevlina.budgetplus.core.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure

expect val revenueCatApiKey: String

fun configureRevenueCat() {
    Purchases.configure(apiKey = revenueCatApiKey)
}