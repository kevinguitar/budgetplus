package com.kevlina.budgetplus.app

import android.app.Application
import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

@DependencyGraph(AppScope::class)
interface BudgetPlusAppGraph : ViewModelGraph {

    @Binds val BudgetPlusApp.bindApplication: Application
    @Binds val BudgetPlusApp.bindContext: Context

    fun inject(app: BudgetPlusApp)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides app: BudgetPlusApp): BudgetPlusAppGraph
    }
}