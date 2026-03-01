package com.kevlina.budgetplus.feature.unlock.premium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.utils.metroViewModel
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions

@Composable
fun PremiumScreen(navController: NavController<BookDest>) {

    val vm = metroViewModel<PremiumViewModel>()
    val isPremium by vm.isPremium.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = isPremium) {
        if (isPremium) {
            navController.navigateUp()
        }
    }

    val options = remember {
        PaywallOptions(
            dismissRequest = navController::navigateUp,
        ) {
            listener = vm.listener
        }
    }

    Paywall(options)
}