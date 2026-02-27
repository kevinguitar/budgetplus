package com.kevlina.budgetplus.book

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevlina.budgetplus.book.ui.BookBinding
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.utils.LocalViewModelGraphProvider
import com.kevlina.budgetplus.core.utils.metroViewModel
import kotlinx.coroutines.flow.collect
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewController
import platform.UIKit.setStatusBarStyle

fun MainViewController(): UIViewController = ComposeUIViewController {
    val graph = BudgetPlusIosAppGraphHolder.graph
    val themeColors by graph.themeManager.themeColors.collectAsStateWithLifecycle()

    LaunchedEffect(themeColors) {
        val isTopBarBgLight = themeColors.primary.luminance() > 0.6
        val statusBarStyle = if (isTopBarBgLight) {
            UIStatusBarStyleDarkContent
        } else {
            UIStatusBarStyleLightContent
        }
        UIApplication.sharedApplication.setStatusBarStyle(statusBarStyle, animated = true)
    }

    LaunchedEffect(graph.navigation) {
        graph.navigation
            .consumeEach { it.navigate() }
            .collect()
    }

    CompositionLocalProvider(
        LocalViewModelGraphProvider provides graph.viewModelGraphProvider
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
