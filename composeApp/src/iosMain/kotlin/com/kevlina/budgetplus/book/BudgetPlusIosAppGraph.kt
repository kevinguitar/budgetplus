package com.kevlina.budgetplus.book

import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.di.ViewModelGraphProvider
import com.kevlina.budgetplus.core.common.nav.NavigationFlow
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.theme.ThemeManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph

@DependencyGraph(AppScope::class)
interface BudgetPlusIosAppGraph {
    val appStartActions: Set<AppStartAction>
    val viewModelGraphProvider: ViewModelGraphProvider
    val authManager: AuthManager
    val themeManager: ThemeManager
    val navigation: NavigationFlow
}

object BudgetPlusIosAppGraphHolder {
    val graph by lazy { createGraph<BudgetPlusIosAppGraph>() }
}