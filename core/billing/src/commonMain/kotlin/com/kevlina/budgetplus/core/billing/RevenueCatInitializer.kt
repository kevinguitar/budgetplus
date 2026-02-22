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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

@ContributesIntoSet(AppScope::class)
class RevenueCatInitializer(
    private val authManager: AuthManager,
    private val billingController: Lazy<BillingController>,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : AppStartAction {

    override fun onAppStart() {
        authManager.userState
            .mapNotNull { it?.id }
            .distinctUntilChanged()
            .onEach { userId ->
                Purchases.logLevel = LogLevel.DEBUG
                Purchases.configure(apiKey = revenueCatApiKey) {
                    appUserId = userId
                }

                Purchases.sharedInstance.getCustomerInfo(
                    onError = { error -> Logger.e { "Error fetching customer info: $error" } },
                    onSuccess = { customerInfo ->
                        billingController.value.onNewCustomerInfo(customerInfo)
                    }
                )
            }
            .launchIn(appScope)
    }
}