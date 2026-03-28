package com.kevlina.budgetplus.insider.app.main

import com.kevlina.budgetplus.core.common.nav.InsiderDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.LogoutNavigation
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
class InsiderLogoutNavigation(
    private val navController: NavController<InsiderDest>,
) : LogoutNavigation {

    override fun navigate() {
        navController.selectRootAndClearAll(InsiderDest.Auth)
    }
}