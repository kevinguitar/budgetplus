package com.kevlina.budgetplus.core.billing

import com.revenuecat.purchases.kmp.models.CustomerInfo
import kotlinx.coroutines.flow.StateFlow

interface BillingController {

    val pricingMap: StateFlow<Map<PremiumPlan, Pricing?>>

    fun onNewCustomerInfo(customerInfo: CustomerInfo)

    fun fetchPrices()
    fun purchase(plan: PremiumPlan)
    fun restorePurchases()
}