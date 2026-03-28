package com.kevlina.budgetplus.book.di

import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface NavigationInitActionProvider {

    @Provides @IntoSet
    fun navControllerInit(
        navController: NavController<BookDest>,
        authManager: AuthManager,
        bookRepo: BookRepo,
    ): AppStartAction = AppStartAction {
        val initialDest = when {
            authManager.userState.value == null -> BookDest.Auth()
            bookRepo.currentBookId == null -> BookDest.Welcome
            else -> BookDest.Record
        }
        navController.selectRootAndClearAll(initialDest)
    }
}