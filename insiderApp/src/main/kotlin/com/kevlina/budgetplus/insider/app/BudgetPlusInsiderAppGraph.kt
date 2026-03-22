package com.kevlina.budgetplus.insider.app

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides

@DependencyGraph(AppScope::class)
interface BudgetPlusInsiderAppGraph {

    @Binds val BudgetPlusInsiderApp.bindApplication: Application
    @Binds val BudgetPlusInsiderApp.bindContext: Context

    // Collect it here to avoid metro's SuspiciousUnusedMultibinding warning, we don't need to invoke them.
    // val appStartActions: Set<AppStartAction>

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides app: BudgetPlusInsiderApp): BudgetPlusInsiderAppGraph
    }
}