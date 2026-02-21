package com.kevlina.budgetplus.feature.unlock.premium

import androidx.lifecycle.ViewModel
import com.kevlina.budgetplus.core.billing.BillingController
import com.kevlina.budgetplus.core.billing.PremiumPlan
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.data.AuthManager
import dev.zacsweers.metro.ContributesIntoMap

@ViewModelKey(PremiumViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class PremiumViewModel(
    private val billingController: BillingController,
    authManager: AuthManager,
) : ViewModel() {

    val isPremium = authManager.isPremium

    val pricingMap = billingController.pricingMap

    init {
        billingController.fetchPrices()
    }

    fun purchase(plan: PremiumPlan) {
        billingController.purchase(plan)
    }

    fun restorePurchases() {
        billingController.restorePurchases()
    }
}