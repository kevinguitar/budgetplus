package com.kevlina.budgetplus.book.di

import com.kevlina.budgetplus.core.billing.BillingController
import com.kevlina.budgetplus.core.billing.PurchaseState
import com.kevlina.budgetplus.feature.speak.record.RecordActor
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.StateFlow

@ContributesBinding(AppScope::class)
class SpeakToRecordImpl : SpeakToRecord {
    override val isAvailableOnDevice: Boolean
        get() = false

    override fun startRecording(): RecordActor {
        TODO("Not yet implemented")
    }
}

@ContributesBinding(AppScope::class)
class BillingControllerImpl : BillingController {
    override val premiumPricing: StateFlow<String?>
        get() = TODO("Not yet implemented")
    override val purchaseState: StateFlow<PurchaseState>
        get() = TODO("Not yet implemented")

    override fun buyPremium() {
        TODO("Not yet implemented")
    }

    override fun endConnection() {
        TODO("Not yet implemented")
    }
}
