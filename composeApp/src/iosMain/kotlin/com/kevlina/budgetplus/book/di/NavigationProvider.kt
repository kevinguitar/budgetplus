package com.kevlina.budgetplus.book.di

import androidx.lifecycle.SavedStateHandle
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BottomNavTab
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.common.nav.NavigationAction
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface NavigationProvider {

    @Provides
    @Named("welcome")
    fun provideWelcomeNavigation(navController: NavController<BookDest>): NavigationAction =
        NavigationAction { navController.selectRootAndClearAll(BookDest.Welcome) }

    @Provides
    @Named("book")
    fun provideBookNavigation(navController: NavController<BookDest>): NavigationAction =
        NavigationAction { navController.selectRootAndClearAll(BookDest.Record) }

    @Provides
    @Named("auth")
    fun provideAuthNavigation(navController: NavController<BookDest>): NavigationAction =
        NavigationAction { navController.selectRootAndClearAll(BookDest.Auth) }

    @SingleIn(AppScope::class)
    @Provides
    fun provideNavController(): NavController<BookDest> {
        return NavController(
            startRoot = BottomNavTab.Add.root,
            serializer = BookDest.serializer(),
            savedStateHandle = SavedStateHandle()
        )
    }

    @Provides @IntoSet
    fun navControllerInit(
        navController: NavController<BookDest>,
        authManager: AuthManager,
        bookRepo: BookRepo,
    ): AppStartAction = AppStartAction {
        val initialDest = when {
            authManager.userState.value == null -> BookDest.Auth
            bookRepo.currentBookId == null -> BookDest.Welcome
            else -> BookDest.Record
        }
        navController.selectRootAndClearAll(initialDest)
    }
}