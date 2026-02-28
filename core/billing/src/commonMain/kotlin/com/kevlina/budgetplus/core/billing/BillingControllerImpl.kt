package com.kevlina.budgetplus.core.billing

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.premium_acknowledge_fail
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.PurchaseRepo
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PeriodUnit
import com.revenuecat.purchases.kmp.models.PricingPhase
import com.revenuecat.purchases.kmp.models.VerificationResult
import com.revenuecat.purchases.kmp.models.freePhase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class BillingControllerImpl(
    private val snackbarSender: SnackbarSender,
    private val tracker: Tracker,
    private val authManager: AuthManager,
    private val purchaseRepo: PurchaseRepo,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : BillingController {

    private val packages = MutableStateFlow<List<Package>?>(null)

    final override val pricingMap: StateFlow<Map<PremiumPlan, Pricing?>>
        field = MutableStateFlow(PremiumPlan.entries.associateWith { null })

    override fun onNewCustomerInfo(customerInfo: CustomerInfo) {
        customerInfo.verifyEntitlements()
    }

    override fun fetchPrices() {
        Purchases.sharedInstance.getOfferings(
            onError = { error ->
                appScope.launch { snackbarSender.send(error.message) }
                Logger.e { "Error fetching prices: $error" }
            },
            onSuccess = { offerings ->
                val storePackages = offerings.current?.availablePackages.orEmpty()
                packages.value = storePackages
                parsePricingPlans(storePackages)
            }
        )
    }

    private fun parsePricingPlans(storePackages: List<Package>) {
        if (storePackages.isEmpty()) return

        pricingMap.value = PremiumPlan.entries.associateWith { plan ->
            val pkg = when (plan) {
                PremiumPlan.Monthly -> storePackages.find { it.packageType == PackageType.MONTHLY }
                PremiumPlan.Annual -> storePackages.find { it.packageType == PackageType.ANNUAL }
                PremiumPlan.Lifetime -> storePackages.find { it.packageType == PackageType.LIFETIME }
            } ?: return@associateWith null

            Pricing(
                discountedPrice = null,
                formattedPrice = pkg.storeProduct.price.formatted,
                freeTrialDays = pkg.storeProduct.subscriptionOptions?.freeTrial?.freePhase?.days
            )
        }
    }

    override fun purchase(plan: PremiumPlan) {
        val packageType = when (plan) {
            PremiumPlan.Monthly -> PackageType.MONTHLY
            PremiumPlan.Annual -> PackageType.ANNUAL
            PremiumPlan.Lifetime -> PackageType.LIFETIME
        }
        val packageToPurchase = packages.value?.find { it.packageType == packageType } ?: return
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
                customerInfo.verifyEntitlements(storeTransaction.transactionId)
                tracker.logEvent("buy_premium_success")
            }
        )

        val event = when (plan) {
            PremiumPlan.Monthly -> "buy_premium_monthly_attempt"
            PremiumPlan.Annual -> "buy_premium_annual_attempt"
            PremiumPlan.Lifetime -> "buy_premium_lifetime_attempt"
        }
        tracker.logEvent(event)
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
            onSuccess = { customerInfo -> customerInfo.verifyEntitlements() }
        )
        tracker.logEvent("restore_purchases_attempt")
    }

    private val PricingPhase.days: Int
        get() = when (billingPeriod.unit) {
            PeriodUnit.DAY -> billingPeriod.value
            PeriodUnit.WEEK -> billingPeriod.value * 7
            PeriodUnit.MONTH -> billingPeriod.value * 30
            PeriodUnit.YEAR -> billingPeriod.value * 365
            PeriodUnit.UNKNOWN -> 0
        }

    private fun CustomerInfo.verifyEntitlements(transactionId: String? = null) {
        if (entitlements.all.isEmpty()) return
        if (entitlements.verification == VerificationResult.FAILED) {
            Logger.e { "Entitlement verification failed for user ${authManager.userId}" }
            appScope.launch { snackbarSender.send(Res.string.premium_acknowledge_fail) }
            return
        }

        appScope.launch {
            val entitlement = entitlements[PREMIUM_ENTITLEMENT]
            if (entitlement?.isActive == true) {
                if (transactionId != null) {
                    purchaseRepo.recordPurchase(
                        orderId = transactionId,
                        productId = entitlement.productIdentifier,
                        client = purchasedClient
                    )
                    tracker.logEvent("buy_premium_success")
                }
                authManager.markPremium(true)
            } else {
                authManager.markPremium(false)
            }
        }
    }
}