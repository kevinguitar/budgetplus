package com.kevlina.budgetplus.core.data

import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.data.remote.Purchase
import com.kevlina.budgetplus.core.data.remote.PurchasesDb
import dev.gitlive.firebase.firestore.CollectionReference
import dev.zacsweers.metro.Inject
import kotlin.time.Clock

@Inject
class PurchaseRepo(
    private val authManager: AuthManager,
    @PurchasesDb private val purchasesDb: Lazy<CollectionReference>,
) {
    suspend fun recordPurchase(
        orderId: String,
        productId: String,
    ) {
        try {
            purchasesDb.value.add(Purchase(
                orderId = orderId,
                productId = productId,
                userId = authManager.userId,
                purchasedOn = Clock.System.now().toEpochMilliseconds()
            ))
        } catch (e: Exception) {
            Logger.e(e) { "Failed to record purchase for order $orderId" }
        }
    }
}