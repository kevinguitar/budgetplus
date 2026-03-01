package com.kevlina.budgetplus.feature.unlock.premium

import androidx.lifecycle.ViewModel
import com.kevlina.budgetplus.core.billing.BillingController
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import com.kevlina.budgetplus.core.data.AuthManager
import dev.zacsweers.metro.ContributesIntoMap

@ViewModelKey(PremiumViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
class PremiumViewModel(
    authManager: AuthManager,
    billingController: BillingController,
) : ViewModel() {
    val isPremium = authManager.isPremium
    val listener = billingController.paywallListener
}