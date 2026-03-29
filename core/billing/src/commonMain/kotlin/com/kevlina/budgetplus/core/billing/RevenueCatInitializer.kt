package com.kevlina.budgetplus.core.billing

import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.data.AuthManager
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.configure
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
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
        Purchases.logLevel = LogLevel.DEBUG
        authManager.userState
            .mapNotNull {
                val userId = it?.id
                if (userId == null) {
                    Purchases.configure(apiKey = BuildKonfig.revenuecatApiKey)
                    Purchases.sharedInstance.delegate = null
                }
                userId
            }
            .distinctUntilChanged()
            .onEach { userId ->
                Purchases.configure(apiKey = BuildKonfig.revenuecatApiKey) {
                    appUserId = userId
                }

                // React to customer info updates from RevenueCat
                Purchases.sharedInstance.delegate = object : PurchasesDelegate {
                    override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
                        billingController.value.onNewCustomerInfo(customerInfo)
                    }

                    override fun onPurchasePromoProduct(
                        product: StoreProduct,
                        startPurchase: (
                            onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
                            onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
                        ) -> Unit,
                    ) = Unit
                }
            }
            .launchIn(appScope)
    }
}