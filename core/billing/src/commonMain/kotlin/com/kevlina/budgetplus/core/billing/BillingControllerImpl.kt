package com.kevlina.budgetplus.core.billing

import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.mapState
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@ContributesBinding(AppScope::class)
class BillingControllerImpl(
    private val snackbarSender: SnackbarSender,
    private val tracker: Tracker,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : BillingController {
    override val premiumPricing: StateFlow<String?> get() = TODO("Not yet implemented")
    override val purchaseState: StateFlow<PurchaseState> get() = TODO("Not yet implemented")
    override fun buyPremium() = TODO("Not yet implemented")
    override fun endConnection() = TODO("Not yet implemented")

    private val packages = MutableStateFlow<List<Package>?>(null)

    override val monthlyPrice: StateFlow<String?> = packages.mapState { packages ->
        packages?.find { it.packageType == PackageType.MONTHLY }?.storeProduct?.price?.formatted
    }

    override val annualPrice: StateFlow<String?> = packages.mapState { packages ->
        packages?.find { it.packageType == PackageType.ANNUAL }?.storeProduct?.price?.formatted
    }

    init {
        fetchPrices()
    }

    fun fetchPrices() {
        Purchases.sharedInstance.getOfferings(
            onError = { error ->
                appScope.launch { snackbarSender.send(error.message) }
                Logger.e { "Error fetching prices: $error" }
            },
            onSuccess = { offerings ->
                packages.value = offerings.current?.availablePackages.orEmpty()
                    .filter { it.storeProduct.id == "budgetplus.premium" }
            }
        )
    }


    override fun buyMonthly() {
        purchase(PackageType.MONTHLY)
    }

    override fun buyAnnual() {
        purchase(PackageType.ANNUAL)
    }

    private fun purchase(type: PackageType) {
        val packageToPurchase = packages.value?.find { it.packageType == type } ?: return
        Purchases.sharedInstance.purchase(
            packageToPurchase = packageToPurchase,
            onError = { error, userCancelled ->
                if (!userCancelled) {
                    appScope.launch { snackbarSender.send(error.message) }
                    Logger.e { "Error purchasing: $error" }
                    tracker.logEvent(
                        event = "buy_premium_fail",
                        params = mapOf("reason" to error.code.description)
                    )
                }
            },
            onSuccess = { storeTransaction, customerInfo ->
                if (customerInfo.entitlements["my_entitlement_identifier"]?.isActive == true) {
                    //TODO: Unlock that great "pro" content
                }
                //TODO: write to purchase db
                tracker.logEvent("buy_premium_success")
            }
        )
    }

    override fun restorePurchases() {
        Purchases.sharedInstance.restorePurchases(
            onError = { error ->
                appScope.launch { snackbarSender.send(error.message) }
                Logger.e { "Error restoring purchases: $error" }
                tracker.logEvent(
                    event = "restore_premium_fail",
                    params = mapOf("reason" to error.code.description)
                )
            },
            onSuccess = { customerInfo ->
                if (customerInfo.entitlements["my_entitlement_identifier"]?.isActive == true) {
                    //TODO: Unlock that great "pro" content
                }
            }
        )
    }
}