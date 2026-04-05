package com.kevlina.budgetplus.insider.app.main

import com.kevlina.budgetplus.core.common.nav.InsiderDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.feature.auth.AuthSuccessNavigation
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
internal class InsiderAuthSuccessNavigation(
    private val navController: NavController<InsiderDest>,
) : AuthSuccessNavigation {

    override suspend fun navigate() {
        navController.selectRootAndClearAll(InsiderDest.Insider)
    }
}