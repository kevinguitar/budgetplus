package com.kevlina.budgetplus.core.common.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BottomNavTab
import com.kevlina.budgetplus.core.common.nav.NavController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
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

@ContributesTo(ViewModelScope::class)
interface NavControllerProvider {

    @SingleIn(ViewModelScope::class)
    @Provides
    fun provideNavController(savedStateHandle: SavedStateHandle): NavController<BookDest> {
        return NavController(
            startRoot = BottomNavTab.Add.root,
            serializer = BookDest.serializer(),
            savedStateHandle = savedStateHandle
        )
    }
}