package com.kevlina.budgetplus.feature.unlock.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.premium_unlock
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.TopBar
import com.kevlina.budgetplus.core.utils.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun PremiumScreen(navController: NavController<BookDest>) {

    val vm = metroViewModel<PremiumViewModel>()
    val pricingMap by vm.pricingMap.collectAsStateWithLifecycle()

    // Close the screen in case user enters it from the deeplink
    LaunchedEffect(key1 = Unit) {
        if (vm.isPremium.value) {
            navController.navigateUp()
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

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1F)
                .fillMaxWidth()
        ) {
            PremiumContent(
                pricingMap = pricingMap,
                purchase = vm::purchase,
                restorePurchases = vm::restorePurchases
            )
        }
    }
}