package com.kevlina.budgetplus.feature.unlock.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.premium_unlock
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.TopBar
import com.kevlina.budgetplus.core.utils.metroViewModel
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions
import org.jetbrains.compose.resources.stringResource

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalAppColors.current.light)
    ) {
        TopBar(
            title = stringResource(Res.string.premium_unlock),
            navigateUp = navController::navigateUp
        )

        Paywall(options)
    }
}