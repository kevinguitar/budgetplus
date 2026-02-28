package com.kevlina.budgetplus.feature.auth

import androidx.lifecycle.ViewModel
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.auth_success
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Toaster
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.nav.NavigationAction
import com.kevlina.budgetplus.core.common.nav.NavigationFlow
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.data.BookRepo
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthResult
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.getString

expect class AuthViewModel: ViewModel {
    val commonAuthViewModel: CommonAuthViewModel

    fun signInWithGoogle()
    fun signInWithApple()
}

@Inject
class CommonAuthViewModel(
    val navigation: NavigationFlow,
    val snackbarSender: SnackbarSender,
    private val bookRepo: BookRepo,
    private val toaster: Toaster,
    private val tracker: Tracker,
    @Named("welcome") private val welcomeNavigationAction: NavigationAction,
    @Named("book") private val bookNavigationAction: NavigationAction,
) {
    val isLoading: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private val auth: FirebaseAuth by lazy { Firebase.auth }

    suspend fun proceedGoogleSignInWithIdToken(idToken: String, accessToken: String?) {
        tracker.logEvent("sign_in_with_google")

        val credential = GoogleAuthProvider.credential(idToken, accessToken)
        try {
            val result = auth.signInWithCredential(credential)
            onLoginCompleted(result)
        } catch (e: Exception) {
            snackbarSender.sendError(e)
        }
    }

    suspend fun proceedAppleSignIn(idToken: String, rawNonce: String) {
        tracker.logEvent("sign_in_with_apple")

        val credential = OAuthProvider.credential(
            providerId = "apple.com",
            idToken = idToken,
            rawNonce = rawNonce
        )
        try {
            isLoading.value = true
            val result = auth.signInWithCredential(credential)
            onLoginCompleted(result)
        } catch (e: Exception) {
            snackbarSender.sendError(e)
        } finally {
            isLoading.value = false
        }
    }

    private suspend fun onLoginCompleted(result: AuthResult) {
        val isNewUser = result.additionalUserInfo?.isNewUser == true
        tracker.logEvent(if (isNewUser) "sign_up" else "login")
        redirectUser(result.user?.displayName.orEmpty())
    }

    private suspend fun redirectUser(name: String) {
        isLoading.value = true

        if (name.isNotBlank()) {
            val message = getString(Res.string.auth_success, name)
            toaster.showMessage(message)
        }

        val action = try {
            if (bookRepo.isUserHasBooks()) {
                bookNavigationAction
            } else {
                welcomeNavigationAction
            }
        } catch (e: Exception) {
            snackbarSender.sendError(e)
            welcomeNavigationAction
        } finally {
            isLoading.value = false
        }
        navigation.sendEvent(action)
    }
}