package com.kevlina.budgetplus.core.common.di

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.MapKey
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import kotlin.reflect.KClass

abstract class ViewModelScope private constructor()

/**
 * Empty interface - only used as a key so we can bind ViewModel @AssistedFactory-annotated
 * implementations into a map. See [com.kevlina.budgetplus.core.utils.assistedMetroViewModel] and
 * [ViewModelGraph.assistedFactoryProviders].
 */
interface ViewModelAssistedFactory

// Used to inject ViewModel instances into ViewModelGraph
@MapKey
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ViewModelKey(val value: KClass<out ViewModel>)

// Used to inject assisted ViewModel factory instances into ViewModelGraph
@MapKey
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AssistedFactoryKey(val value: KClass<out ViewModelAssistedFactory>)

@GraphExtension(ViewModelScope::class)
interface ViewModelGraph {

    @Multibinds
    val viewModelProviders: Map<KClass<out ViewModel>, Provider<ViewModel>>

    @Multibinds(allowEmpty = true)
    val assistedFactoryProviders:
        Map<KClass<out ViewModelAssistedFactory>, Provider<ViewModelAssistedFactory>>

    @Provides
    fun provideSavedStateHandle(creationExtras: CreationExtras): SavedStateHandle =
        creationExtras.createSavedStateHandle()

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    fun interface Factory {
        fun create(@Provides extras: CreationExtras): ViewModelGraph
    }
}

fun interface ViewModelGraphProvider : ViewModelProvider.Factory {
    fun buildViewModelGraph(extras: CreationExtras): ViewModelGraph
}