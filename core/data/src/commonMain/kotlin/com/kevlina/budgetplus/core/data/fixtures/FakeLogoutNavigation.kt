package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.VisibleForTesting
import com.kevlina.budgetplus.core.data.LogoutNavigation

@VisibleForTesting
class FakeLogoutNavigation : LogoutNavigation {

    var navigated = false
        private set

    override fun navigate() {
        navigated = true
    }
}
