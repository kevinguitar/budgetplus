package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.CrashlyticsProvider

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeCrashlyticsProvider : CrashlyticsProvider {

    var lastUserId: String? = null
        private set

    override fun setUserId(userId: String) {
        lastUserId = userId
    }
}
