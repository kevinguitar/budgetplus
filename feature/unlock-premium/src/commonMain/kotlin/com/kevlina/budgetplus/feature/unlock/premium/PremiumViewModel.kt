package com.kevlina.budgetplus.feature.unlock.premium

import androidx.lifecycle.ViewModel
import com.kevlina.budgetplus.core.billing.BillingController
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.AuthManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ViewModelKey
@ContributesIntoMap(AppScope::class)
class PremiumViewModel(
    val navController: NavController<BookDest>,
    authManager: AuthManager,
    billingController: BillingController,
) : ViewModel() {
    val isPremium = authManager.isPremium
    val listener = billingController.paywallListener
}