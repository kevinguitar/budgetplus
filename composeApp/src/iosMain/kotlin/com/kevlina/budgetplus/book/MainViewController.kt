package com.kevlina.budgetplus.book

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevlina.budgetplus.book.ui.BookBinding
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.ui.AppTheme
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collect
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    val graph = BudgetPlusIosAppGraphHolder.graph
    val themeColors by graph.themeManager.themeColors.collectAsStateWithLifecycle()

    CompositionLocalProvider(
        LocalMetroViewModelFactory provides graph.viewModelFactory
    ) {
        AppTheme(themeColors) {
            val vm = metroViewModel<BookViewModel>()

            LaunchedEffect(graph.deeplinkFlow) {
                graph.deeplinkFlow.consumeEach { url ->
                    val type = vm.handleDeeplink(url)
                    if (type == DeeplinkType.JoinRequest) {
                        vm.handleJoinRequest()
                    }
                }.collect()
            }

            BookBinding(vm = vm)
        }
    }
}
