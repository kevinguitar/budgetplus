package com.kevlina.budgetplus.feature.unlock.premium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun UnlockPremiumScreen() {

    val vm = metroViewModel<UnlockPremiumViewModel>()
    val isPremium by vm.isPremium.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = isPremium) {
        if (isPremium) {
            vm.navController.navigateUp()
        }
    }

    val options = remember {
        PaywallOptions(
            dismissRequest = vm.navController::navigateUp,
        ) {
            listener = vm.listener
        }
    }

    Paywall(options)
}