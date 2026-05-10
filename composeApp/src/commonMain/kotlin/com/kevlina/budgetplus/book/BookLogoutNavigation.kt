package com.kevlina.budgetplus.book

import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.LogoutNavigation
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
internal class BookLogoutNavigation(
    private val navController: NavController<BookDest>,
) : LogoutNavigation {

    override fun navigate() {
        navController.selectRootAndClearAll(BookDest.Auth(enableAutoSignIn = false))
    }
}