package com.kevlina.budgetplus.core.billing

import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallListener

interface BillingController {

    val paywallListener: PaywallListener

    fun onNewCustomerInfo(customerInfo: CustomerInfo)

}