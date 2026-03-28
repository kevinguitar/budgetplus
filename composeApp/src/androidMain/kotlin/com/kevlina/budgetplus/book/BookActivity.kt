package com.kevlina.budgetplus.book

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.app_update_downloaded
import budgetplus.core.common.generated.resources.cta_complete
import com.kevlina.budgetplus.book.ui.BookBinding
import com.kevlina.budgetplus.core.common.SnackbarDuration
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.di.resolveGraphExtensionFactory
import com.kevlina.budgetplus.core.common.nav.APP_DEEPLINK
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NAV_SETTINGS_PATH
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.theme.ThemeManager
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.utils.setStatusBarColor
import com.kevlina.budgetplus.inapp.update.InAppUpdateManager
import com.kevlina.budgetplus.inapp.update.InAppUpdateState
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory

class BookActivity : ComponentActivity() {

    @Inject private lateinit var authManager: AuthManager
    @Inject private lateinit var bookRepo: BookRepo
    @Inject private lateinit var themeManager: ThemeManager
    @Inject private lateinit var inAppUpdateManager: InAppUpdateManager
    @Inject private lateinit var snackbarSender: SnackbarSender
    @Inject private lateinit var viewModelFactory: MetroViewModelFactory
    @Inject private lateinit var navController: NavController<BookDest>

    private val viewModel by viewModels<BookViewModel>()

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = viewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        resolveGraphExtensionFactory<BookActivityGraph.Factory>()
            .create(this)
            .inject(this)

        // When the user open the settings from app preference.
        if (intent.action == Intent.ACTION_APPLICATION_PREFERENCES) {
            intent.data = "$APP_DEEPLINK/$NAV_SETTINGS_PATH".toUri()
        }

        enableEdgeToEdge()
        setStatusBarColor(isLight = false)
        super.onCreate(savedInstanceState)

        viewModel.handleDeeplink(intent.dataString)

        val destination = when {
            authManager.userState.value == null -> BookDest.Auth()
            bookRepo.currentBookId == null -> BookDest.Welcome
            else -> null
        }

        if (destination != null) {
            navController.selectRootAndClearAll(destination)
        }

        setContent {
            CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
                val themeColors by themeManager.themeColors.collectAsStateWithLifecycle()
                AppTheme(themeColors) {
                    BookBinding(vm = viewModel)
                }

                val appUpdateState by inAppUpdateManager.updateState.collectAsStateWithLifecycle()
                LaunchedEffect(key1 = appUpdateState) {
                    if (appUpdateState is InAppUpdateState.Downloaded) {
                        snackbarSender.send(
                            message = Res.string.app_update_downloaded,
                            actionLabel = Res.string.cta_complete,
                            duration = SnackbarDuration.Indefinite,
                            action = (appUpdateState as InAppUpdateState.Downloaded).complete
                        )
                    }
                }
            }
        }

        addOnNewIntentListener { newIntent ->
            viewModel.handleDeeplink(newIntent.dataString)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.handleJoinRequest()
    }
}