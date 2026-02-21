package com.kevlina.budgetplus.core.billing

import kotlinx.coroutines.flow.StateFlow

interface BillingController {

    val pricingMap: StateFlow<Map<PremiumPlan, Pricing?>>

    fun fetchPrices()
    fun purchase(plan: PremiumPlan)
    fun restorePurchases()
}