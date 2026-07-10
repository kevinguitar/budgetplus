package com.kevlina.budgetplus.feature.unlock.premium

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevlina.budgetplus.core.ui.InfiniteCircularProgress
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun UnlockPremiumScreen() {

    val vm = metroViewModel<UnlockPremiumViewModel>()
    val isPremium by vm.isPremium.collectAsStateWithLifecycle()
    var isRevenueCatConfigured by remember { mutableStateOf(Purchases.isConfigured) }

    LaunchedEffect(key1 = isPremium) {
        if (isPremium) {
            vm.navController.navigateUp()
        }
    }

    LaunchedEffect(Unit) {
        // RevenueCat is initialized asynchronously based on the user state, and the SDK doesn't provide a callback
        // for this, so poll it until the SDK is initialized to render a paywall.
        while (!isRevenueCatConfigured) {
            isRevenueCatConfigured = Purchases.isConfigured
            delay(100.milliseconds)
        }
    }

    val options = remember {
        PaywallOptions(
            dismissRequest = vm.navController::navigateUp,
        ) {
            listener = vm.listener
        }
    }

    if (isRevenueCatConfigured) {
        Paywall(options)
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            InfiniteCircularProgress()
        }
    }
}