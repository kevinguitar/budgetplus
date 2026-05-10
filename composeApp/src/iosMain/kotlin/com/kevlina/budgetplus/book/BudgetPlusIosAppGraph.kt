package com.kevlina.budgetplus.book

import com.kevlina.budgetplus.book.di.DeeplinkFlow
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.theme.ThemeManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

@DependencyGraph(AppScope::class)
interface BudgetPlusIosAppGraph : ViewModelGraph {
    val appStartActions: Set<AppStartAction>
    val viewModelFactory: MetroViewModelFactory
    val authManager: AuthManager
    val themeManager: ThemeManager
    val deeplinkFlow: DeeplinkFlow
}

object BudgetPlusIosAppGraphHolder {
    val graph by lazy { createGraph<BudgetPlusIosAppGraph>() }

    fun onNewDeeplink(deeplink: String?) {
        graph.deeplinkFlow.sendEvent(deeplink)
    }
}