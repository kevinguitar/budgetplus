package com.kevlina.budgetplus.core.billing

import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallListener
import kotlinx.coroutines.flow.StateFlow

interface BillingController {

    val pricingMap: StateFlow<Map<PremiumPlan, Pricing?>>

    val paywallListener: PaywallListener

    fun onNewCustomerInfo(customerInfo: CustomerInfo)

    fun fetchPrices()
    fun purchase(plan: PremiumPlan)
    fun restorePurchases()
}