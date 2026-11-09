package com.kevlina.budgetplus.book.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
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
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.LocalTopBarActionsHost
import com.kevlina.budgetplus.core.ui.NavTransitions
import com.kevlina.budgetplus.core.ui.NavTransitions.pop
import com.kevlina.budgetplus.core.ui.NavTransitions.push
import com.kevlina.budgetplus.core.ui.Scaffold
import com.kevlina.budgetplus.core.ui.SnackbarHost
import com.kevlina.budgetplus.core.ui.TopBarActionsHost
import com.kevlina.budgetplus.core.ui.bubble.Bubble
import com.kevlina.budgetplus.core.ui.safeArea
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
        val navLayout = rememberBookNavLayout()
        // The side bars replace the bottom nav, so they only show where the bottom nav would.
        val sideColumn = (navLayout as? BookNavLayout.SideColumn)?.takeIf { showBottomNav }
        val showRail = navLayout == BookNavLayout.Rail && showBottomNav
        val showBottomBar = navLayout == BookNavLayout.Bottom && showBottomNav
        val topBarActionsHost = remember { TopBarActionsHost() }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    BottomNav(navController, previewColors)
                }
            },
            snackbarHost = {
                SnackbarHost(
                    snackbarData = snackbarData,
                    bottomSpacing = if (sideColumn != null) SideColumnSnackbarSpacing else null,
                )
            },
            // In the side column layout, the system UI is in the column, so the snackbar doesn't
            // need to avoid the bottom insets. Keep the horizontal ones, so it's centered over the
            // content instead of the whole window.
            contentWindowInsets = if (sideColumn != null) {
                WindowInsets.safeArea.only(WindowInsetsSides.Horizontal)
            } else {
                null
            },
        ) { innerPadding ->
            // Keep the same structure for every layout, so rotating or folding the device keeps the
            // state of the screens.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = previewColors?.light ?: LocalAppColors.current.light)
            ) {
                if (showRail) {
                    NavRail(navController = navController, previewColors = previewColors)
                }

                if (sideColumn?.isStart == true) {
                    ComposeOnLayout {
                        SideColumnBar(
                            navController = navController,
                            previewColors = previewColors,
                            isStart = true,
                            actions = topBarActionsHost.actions,
                        )
                    }
                }

                CompositionLocalProvider(
                    LocalTopBarActionsHost provides topBarActionsHost.takeIf { sideColumn != null }
                ) {
                    BookContent(
                        vm = vm,
                        showBannerAd = showBannerAd,
                        previewColors = previewColors,
                        modifier = Modifier
                            .weight(1F)
                            .fillMaxHeight()
                            // Without the bottom nav, the content goes to the bottom edge, e.g. lists
                            // scroll behind the home indicator.
                            .thenIf(showBottomBar) {
                                // Do not consider the top padding, and let TopBar handle it.
                                val bottomPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding())
                                // The bottom nav already covers the bottom insets, so the screens shouldn't
                                // add them again.
                                Modifier
                                    .padding(bottomPadding)
                                    .consumeWindowInsets(bottomPadding)
                            }
                            // The side bars cover the insets on their side.
                            .thenIf(showRail) {
                                Modifier.consumeWindowInsets(WindowInsets.safeArea.only(WindowInsetsSides.Start))
                            }
                            .thenIf(sideColumn != null) {
                                val side = if (sideColumn?.isStart == true) WindowInsetsSides.Start else WindowInsetsSides.End
                                Modifier.consumeWindowInsets(WindowInsets.safeArea.only(side))
                            }
                    )
                }

                if (sideColumn?.isStart == false) {
                    ComposeOnLayout {
                        SideColumnBar(
                            navController = navController,
                            previewColors = previewColors,
                            isStart = false,
                            actions = topBarActionsHost.actions,
                        )
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

/**
 * Whether the target screen is pushed right on top of the current one. Switching the bottom nav
 * tabs reorders the back stack instead, e.g. going back to a tab that has another screen on top of
 * its root, which shouldn't animate like a push.
 */
private fun <T : Any> AnimatedContentTransitionScope<Scene<T>>.isPushedOnTop(): Boolean {
    val currentTop = initialState.entries.lastOrNull() ?: return false
    return targetState.previousEntries.lastOrNull()?.contentKey == currentTop.contentKey
}

private val lightBackgroundNavKeys = setOf(BookDest.Welcome::class, BookDest.UnlockPremium::class)

private val SideColumnSnackbarSpacing = 16.dp

@Composable
private fun BookContent(
    vm: BookViewModel,
    showBannerAd: Boolean,
    previewColors: ThemeColors?,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.background(color = previewColors?.light ?: LocalAppColors.current.light)
    ) {
        NavDisplay(
            backStack = vm.navController.backStack,
            entryProvider = ::bookNavGraph,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            onBack = vm.navController::navigateUp,
            transitionSpec = { if (isPushedOnTop()) push() else NavTransitions.none() },
            popTransitionSpec = { pop() },
            predictivePopTransitionSpec = { pop() },
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

