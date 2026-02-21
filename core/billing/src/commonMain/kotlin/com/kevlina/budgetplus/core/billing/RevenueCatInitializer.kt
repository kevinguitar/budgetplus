package com.kevlina.budgetplus.core.billing

import com.kevlina.budgetplus.core.common.AppStartAction
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet

@ContributesIntoSet(AppScope::class)
class RevenueCatInitializer : AppStartAction {
    override fun onAppStart() {
        configureRevenueCat()
    }
}