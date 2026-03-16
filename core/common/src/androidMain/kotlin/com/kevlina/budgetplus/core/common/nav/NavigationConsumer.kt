package com.kevlina.budgetplus.core.common.nav

import androidx.activity.ComponentActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kevlina.budgetplus.core.common.consumeEach
import kotlinx.coroutines.flow.launchIn

fun ComponentActivity.consumeNavigation(navigationFlow: NavigationFlow) {
    navigationFlow
        .consumeEach { action ->
            action as AndroidNavigationAction

            if (action.intent.component?.className == this::class.qualifiedName) {
                return@consumeEach
            }
            
            startActivity(action.intent)

            if (action.finishCurrent) {
                finish()
            }
        }
        .flowWithLifecycle(lifecycle)
        .launchIn(lifecycleScope)
}