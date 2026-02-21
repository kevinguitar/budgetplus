package com.kevlina.budgetplus.core.billing

import kotlinx.coroutines.flow.StateFlow

interface BillingController {

    //TODO: Stale fields, remove later
    val premiumPricing: StateFlow<String?>
    val purchaseState: StateFlow<PurchaseState>
    fun buyPremium()
    fun endConnection()

    val pricingMap: StateFlow<Map<PremiumPlan, Pricing?>>

    fun purchase(plan: PremiumPlan)
    fun restorePurchases()
}