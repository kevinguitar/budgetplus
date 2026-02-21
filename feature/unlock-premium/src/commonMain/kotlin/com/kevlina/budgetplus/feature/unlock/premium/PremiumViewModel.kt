package com.kevlina.budgetplus.feature.unlock.premium

import androidx.lifecycle.ViewModel
import com.kevlina.budgetplus.core.billing.BillingController
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.data.AuthManager
import dev.zacsweers.metro.ContributesIntoMap

@ViewModelKey(PremiumViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class PremiumViewModel(
    private val billingController: BillingController,
    private val tracker: Tracker,
    authManager: AuthManager,
) : ViewModel() {

    val isPremium = authManager.isPremium

    val monthlyPrice = billingController.monthlyPrice
    val annualPrice = billingController.annualPrice

    fun buyMonthly() {
        billingController.buyMonthly()
        tracker.logEvent("buy_premium_monthly_attempt")
    }

    fun buyAnnual() {
        billingController.buyAnnual()
        tracker.logEvent("buy_premium_annual_attempt")
    }
}