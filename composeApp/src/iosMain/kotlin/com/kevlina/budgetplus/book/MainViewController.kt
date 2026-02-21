package com.kevlina.budgetplus.book

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevlina.budgetplus.book.ui.BookBinding
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.utils.LocalViewModelGraphProvider
import com.kevlina.budgetplus.core.utils.metroViewModel
import kotlinx.coroutines.flow.collect
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.UIViewController
import platform.UIKit.setStatusBarStyle

fun MainViewController(deeplink: String?): UIViewController = ComposeUIViewController {
    val graph = remember { BudgetPlusIosAppGraphHolder.graph }
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

    LaunchedEffect(graph) {
        val initialDest = when {
            graph.authManager.userState.value == null -> BookDest.Auth
            graph.bookRepo.currentBookId == null -> BookDest.Welcome
            else -> BookDest.Record
        }
        graph.navController.selectRootAndClearAll(initialDest)
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
            LaunchedEffect(deeplink) {
                val type = vm.handleDeeplink(deeplink)
                if (type == DeeplinkType.JoinRequest) {
                    vm.handleJoinRequest()
                }
            }

            BookBinding(vm = vm)
        }
    }
}
