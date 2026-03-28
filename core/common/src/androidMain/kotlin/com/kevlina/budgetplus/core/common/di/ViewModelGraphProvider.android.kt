package com.kevlina.budgetplus.core.common.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlin.reflect.KClass
import kotlin.reflect.cast

@ContributesBinding(AppScope::class)
class ViewModelGraphProviderImpl(private val context: Context) : ViewModelGraphProvider {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        val viewModelGraph = buildViewModelGraph(extras)
        val viewModelProvider =
            requireNotNull(viewModelGraph.viewModelProviders[modelClass]) {
                "Unknown model class $modelClass"
            }
        return modelClass.cast(viewModelProvider())
    }

    override fun buildViewModelGraph(extras: CreationExtras): ViewModelGraph =
        context.resolveGraphExtensionFactory<ViewModelGraph.Factory>().create(extras)
}
