package com.kevlina.budgetplus.feature.welcome.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kevlina.budgetplus.core.ui.AdaptiveScreen
import com.kevlina.budgetplus.feature.welcome.WelcomeViewModel

@Composable
fun WelcomeBinding(vm: WelcomeViewModel) {
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        onBackCompleted = vm::logout
    )

    AdaptiveScreen(
        modifier = Modifier.fillMaxSize(),
        regularContent = {
            WelcomeContentRegular(vm)
        },
        wideContent = {
            WelcomeContentWide(vm)
        },
        packedContent = {
            WelcomeContentPacked(vm)
        }
    )
}