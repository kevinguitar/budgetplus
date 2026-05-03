package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.LogoutNavigation

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeLogoutNavigation : LogoutNavigation {

    var navigated = false
        private set

    override fun navigate() {
        navigated = true
    }
}
