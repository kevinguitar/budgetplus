package com.kevlina.budgetplus.core.billing

import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.data.AuthManager
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@ContributesIntoSet(AppScope::class)
class RevenueCatInitializer(
    private val authManager: AuthManager,
    private val billingController: Lazy<BillingController>,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : AppStartAction {

    override fun onAppStart() {
        appScope.launch {
            val user = authManager.userState
                .filterNotNull()
                .first()

            Purchases.logLevel = LogLevel.DEBUG
            Purchases.configure(apiKey = revenueCatApiKey) {
                appUserId = user.id
            }

            Purchases.sharedInstance.getCustomerInfo(
                onError = { error -> Logger.e { "Error fetching customer info: $error" } },
                onSuccess = { customerInfo ->
                    billingController.value.onNewCustomerInfo(customerInfo)
                }
            )
        }
    }
}