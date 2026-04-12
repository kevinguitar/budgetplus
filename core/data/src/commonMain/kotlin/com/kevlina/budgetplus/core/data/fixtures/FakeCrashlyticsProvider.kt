package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.VisibleForTesting
import com.kevlina.budgetplus.core.data.CrashlyticsProvider

@VisibleForTesting
class FakeCrashlyticsProvider : CrashlyticsProvider {

    var lastUserId: String? = null
        private set

    override fun setUserId(userId: String) {
        lastUserId = userId
    }
}
