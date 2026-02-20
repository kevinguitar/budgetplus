package com.kevlina.budgetplus.book

import androidx.lifecycle.SavedStateHandle
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.di.ViewModelGraphProvider
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BottomNavTab
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.common.nav.NavigationFlow
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.theme.ThemeManager
import com.kevlina.budgetplus.notification.FcmServiceDelegate
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraph

@DependencyGraph(AppScope::class)
interface BudgetPlusIosAppGraph {
    // For app start
    val appStartActions: Set<AppStartAction>
    val fcmServiceDelegate: FcmServiceDelegate

    // For UI rendering
    val viewModelGraphProvider: ViewModelGraphProvider
    val themeManager: ThemeManager
    val authManager: AuthManager
    val bookRepo: BookRepo

    // For navigation
    val navController: NavController<BookDest>
    val navigation: NavigationFlow

    @SingleIn(AppScope::class)
    @Provides
    fun provideNavController(): NavController<BookDest> {
        return NavController(
            startRoot = BottomNavTab.Add.root,
            serializer = BookDest.serializer(),
            savedStateHandle = SavedStateHandle()
        )
    }
}

object BudgetPlusIosAppGraphHolder {
    val graph by lazy { createGraph<BudgetPlusIosAppGraph>() }

    init {
        graph.appStartActions.forEach {
            it.onAppStart()
        }
    }
}