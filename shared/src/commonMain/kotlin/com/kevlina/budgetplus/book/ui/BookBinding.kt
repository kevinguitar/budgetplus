package com.kevlina.budgetplus.book.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import coil3.compose.LocalPlatformContext
import com.kevlina.budgetplus.book.BookViewModel
import com.kevlina.budgetplus.core.ads.BannerAd
import com.kevlina.budgetplus.core.ads.HandleInterstitialAd
import com.kevlina.budgetplus.core.common.SnackbarData
import com.kevlina.budgetplus.core.common.UiTestFlags
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.Scaffold
import com.kevlina.budgetplus.core.ui.SnackbarHost
import com.kevlina.budgetplus.core.ui.bubble.Bubble
import com.kevlina.budgetplus.core.ui.safeHorizontal
import com.kevlina.budgetplus.core.ui.thenIf
import kotlinx.coroutines.flow.launchIn

@Composable
internal fun BookBinding(
    vm: BookViewModel,
) {
    val navController = vm.navController

    val showBottomNav by vm.showBottomNav.collectAsStateWithLifecycle()
    val showBannerAd by vm.showBannerAd.collectAsStateWithLifecycle()
    val isEligibleForInterstitialAds by vm.isEligibleForInterstitialAds.collectAsStateWithLifecycle()
    val previewColors by vm.themeManager.previewColors.collectAsStateWithLifecycle()
    val bubbleDest by vm.bubbleViewModel.destination.collectAsStateWithLifecycle()
    val themeColors by vm.themeManager.themeColors.collectAsStateWithLifecycle()

    var snackbarData: SnackbarData? by remember { mutableStateOf(null) }

    // Only consume while started, other activities could show the snackbars, e.g. widget config.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(vm, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.snackbarSender.snackbarEvent
                .consumeEach { snackbarData = it }
                .launchIn(this)
        }
    }

    val platformContext = LocalPlatformContext.current
    LaunchedEffect(themeColors, navController) {
        navController.currentNavKeyFlow.collect { navKey ->
            val bgColor = if (navKey::class in lightBackgroundNavKeys) {
                themeColors.light
            } else {
                themeColors.primary
            }
            setStatusBarColor(context = platformContext, isLightBg = bgColor.luminance() > 0.6)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    BottomNav(navController, previewColors)
                }
            },
            snackbarHost = { SnackbarHost(snackbarData) },
        ) { innerPadding ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .thenIf(showBottomNav) {
                        // Do not consider the top padding, and let TopBar handle it.
                        val bottomPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
                        // The bottom nav already covers the bottom insets, so the screens shouldn't add them again.
                        Modifier
                            .padding(bottomPadding)
                            .consumeWindowInsets(bottomPadding)
                    }
                    .background(color = previewColors?.light ?: LocalAppColors.current.light)
            ) {
                NavDisplay(
                    backStack = vm.navController.backStack,
                    entryProvider = ::bookNavGraph,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    onBack = vm.navController::navigateUp,
                    transitionSpec = { ContentTransform(fadeIn(), fadeOut()) },
                    popTransitionSpec = { ContentTransform(fadeIn(), fadeOut()) },
                    predictivePopTransitionSpec = { ContentTransform(fadeIn(), fadeOut()) },
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxWidth()
                        .background(color = previewColors?.light ?: LocalAppColors.current.light)
                )

                if (showBannerAd) {
                    LaunchedEffect(vm) {
                        vm.admobInitializer.requestTrackingAuthorization()
                    }
                    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.safeHorizontal)) {
                        BannerAd(bannerId = vm.adUnitId.banner)
                    }
                }
            }
        }

        if (!UiTestFlags.enabled) {
            Bubble(
                dest = bubbleDest,
                dismissBubble = vm.bubbleViewModel::dismissBubble
            )
        }

        if (isEligibleForInterstitialAds) {
            HandleInterstitialAd(
                adUnitId = vm.adUnitId.interstitial,
                handler = vm.interstitialAdsHandler
            )
        }
    }
}

private val lightBackgroundNavKeys = setOf(BookDest.Welcome::class, BookDest.UnlockPremium::class)