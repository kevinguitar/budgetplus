package com.kevlina.budgetplus.insider.app.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.kevlina.budgetplus.core.common.di.resolveGraphExtensionFactory
import com.kevlina.budgetplus.core.common.nav.InsiderDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.utils.setStatusBarColor
import com.kevlina.budgetplus.insider.app.main.ui.InsiderBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory

class InsiderActivity : ComponentActivity() {

    @Inject private lateinit var authManager: AuthManager
    @Inject private lateinit var viewModelFactory: MetroViewModelFactory
    @Inject private lateinit var navController: NavController<InsiderDest>

    override fun onCreate(savedInstanceState: Bundle?) {
        resolveGraphExtensionFactory<InsiderActivityGraph.Factory>()
            .create()
            .inject(this)

        enableEdgeToEdge()
        setStatusBarColor(isLightBg = false)
        super.onCreate(savedInstanceState)

        if (authManager.userState.value == null) {
            navController.selectRootAndClearAll(InsiderDest.Auth)
        }

        setContent {
            CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
                AppTheme {
                    InsiderBinding()
                }
            }
        }
    }
}