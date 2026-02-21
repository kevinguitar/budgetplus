package com.kevlina.budgetplus.core.billing

import kotlinx.coroutines.flow.StateFlow

interface BillingController {

    //TODO: Stale fields, remove later
    val premiumPricing: StateFlow<String?>
    val purchaseState: StateFlow<PurchaseState>
    fun buyPremium()
    fun endConnection()

    val monthlyPrice: StateFlow<String?>
    val annualPrice: StateFlow<String?>

    fun buyMonthly()
    fun buyAnnual()
    fun restorePurchases()
}