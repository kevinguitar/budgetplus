package com.kevlina.budgetplus.book.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import com.kevlina.budgetplus.book.BudgetPlusIosAppGraphHolder
import com.kevlina.budgetplus.core.common.di.ViewModelGraph
import com.kevlina.budgetplus.core.common.di.ViewModelGraphProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlin.reflect.KClass
import kotlin.reflect.cast

@ContributesBinding(AppScope::class)
class ViewModelGraphProviderImpl : ViewModelGraphProvider {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        val viewModelGraph = buildViewModelGraph(extras)
        val viewModelProvider =
            requireNotNull(viewModelGraph.viewModelProviders[modelClass]) {
                "Unknown model class $modelClass"
            }
        return modelClass.cast(viewModelProvider())
    }

    override fun buildViewModelGraph(extras: CreationExtras): ViewModelGraph =
        (BudgetPlusIosAppGraphHolder.graph as ViewModelGraph.Factory).create(extras)
}