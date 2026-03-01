package com.kevlina.budgetplus.core.billing

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.premium_acknowledge_fail
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.PurchaseRepo
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.revenuecat.purchases.kmp.models.VerificationResult
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallListener
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
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

    override val paywallListener = object : PaywallListener {
        override fun onPurchaseStarted(rcPackage: Package) {
            val event = when (rcPackage.packageType) {
                PackageType.LIFETIME -> "buy_premium_lifetime_attempt"
                PackageType.ANNUAL -> "buy_premium_annual_attempt"
                PackageType.MONTHLY -> "buy_premium_monthly_attempt"
                else -> "buy_premium_unknown_attempt"
            }
            tracker.logEvent(event)
        }

        override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
            customerInfo.verifyEntitlements(storeTransaction.transactionId)
        }

        override fun onPurchaseError(error: PurchasesError) {
            tracker.logEvent(
                event = "buy_premium_fail",
                params = mapOf("reason" to error.code.description)
            )
        }

        override fun onRestoreStarted() {
            tracker.logEvent("restore_purchases_attempt")
        }
    }

    override fun onNewCustomerInfo(customerInfo: CustomerInfo) {
        customerInfo.verifyEntitlements()
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