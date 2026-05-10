package com.kevlina.budgetplus.core.common.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.common.Tracker

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeTracker : Tracker {

    var lastEvent: Pair<String, Map<String, Any>?>? = null

    val lastEventName get() = lastEvent?.first

    override fun logEvent(event: String, params: Map<String, Any>?) {
        lastEvent = event to params
    }
}