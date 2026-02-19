package com.kevlina.budgetplus.book.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.lexilabs.basic.ads.BasicAds
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.InterstitialAd
import app.lexilabs.basic.ads.composable.rememberInterstitialAd
import com.kevlina.budgetplus.book.BookViewModel
import com.kevlina.budgetplus.core.common.SnackbarData
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.Scaffold
import com.kevlina.budgetplus.core.ui.SnackbarHost
import com.kevlina.budgetplus.core.ui.bubble.Bubble
import com.kevlina.budgetplus.core.ui.thenIf
import kotlinx.coroutines.flow.launchIn

@OptIn(DependsOnGoogleMobileAds::class)
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

    var snackbarData: SnackbarData? by remember { mutableStateOf(null) }

    // Initialize admob sdk
    BasicAds.Initialize()

    LaunchedEffect(vm) {
        vm.snackbarSender.snackbarEvent
            .consumeEach { snackbarData = it }
            .launchIn(this)
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
                        Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    }
                    .background(color = previewColors?.light ?: LocalAppColors.current.light)
            ) {
                NavDisplay(
                    backStack = vm.navController.backStack,
                    entryProvider = { bookNavGraph(navController, it) },
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
                    AdsBanner(bannerId = vm.adUnitId.banner)
                }
            }
        }

        Bubble(
            dest = bubbleDest,
            dismissBubble = vm.bubbleViewModel::dismissBubble
        )

        if (isEligibleForInterstitialAds) {
            val interstitialAd by rememberInterstitialAd(adUnitId = vm.adUnitId.interstitial)
            var showInterstitialAd by remember { mutableStateOf(false) }

            LaunchedEffect(vm) {
                vm.interstitialAdsHandler.showAdEvent
                    .consumeEach { showInterstitialAd = true }
                    .launchIn(this)
            }

            if (showInterstitialAd) {
                InterstitialAd(
                    loadedAd = interstitialAd,
                    onDismissed = { showInterstitialAd = false }
                )
            }
        }
    }
}