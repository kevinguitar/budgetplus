package com.kevlina.budgetplus.insider.app.main.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kevlina.budgetplus.core.common.SnackbarData
import com.kevlina.budgetplus.core.common.consumeEach
import com.kevlina.budgetplus.core.common.nav.InsiderDest
import com.kevlina.budgetplus.core.ui.NavTransitions.pop
import com.kevlina.budgetplus.core.ui.NavTransitions.push
import com.kevlina.budgetplus.core.ui.Scaffold
import com.kevlina.budgetplus.core.ui.SnackbarHost
import com.kevlina.budgetplus.feature.auth.AuthViewModel
import com.kevlina.budgetplus.feature.auth.ui.AuthBinding
import com.kevlina.budgetplus.feature.insider.InsiderScreen
import com.kevlina.budgetplus.feature.push.notifications.PushNotificationsScreen
import com.kevlina.budgetplus.insider.app.main.InsiderRootViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.launchIn

@Composable
internal fun InsiderBinding(vm: InsiderRootViewModel = metroViewModel()) {

    val navController = vm.navController
    var snackbarData: SnackbarData? by remember { mutableStateOf(null) }

    LaunchedEffect(vm) {
        vm.snackbarSender.snackbarEvent
            .consumeEach { snackbarData = it }
            .launchIn(this)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarData) },
        ) { _ ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                NavDisplay(
                    backStack = navController.backStack,
                    entryProvider = entryProvider {
                        entry<InsiderDest.Auth> {
                            val vm = metroViewModel<AuthViewModel>()
                            LaunchedEffect(vm) {
                                vm.checkAuthorizedAccounts(enableAutoSignIn = false)
                            }
                            AuthBinding(
                                vm = vm.commonAuthViewModel,
                                signInWithGoogle = vm::signInWithGoogle,
                                signInWithApple = vm::signInWithApple,
                            )
                        }

                        entry<InsiderDest.Insider> {
                            InsiderScreen(openPushNotifications = {
                                navController.navigate(InsiderDest.PushNotifications)
                            })
                        }

                        entry<InsiderDest.PushNotifications> {
                            PushNotificationsScreen(navigateUp = navController::navigateUp)
                        }
                    },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    transitionSpec = { push() },
                    popTransitionSpec = { pop() },
                    predictivePopTransitionSpec = { pop() },
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxWidth()
                )
            }
        }
    }
}