package com.kevlina.budgetplus.feature.auth

import androidx.lifecycle.ViewModel
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.auth_success
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.BookRepo
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthResult
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.getString

expect val isAppleSignInAvailable: Boolean

expect class AuthViewModel : ViewModel {
    val commonAuthViewModel: CommonAuthViewModel

    fun checkAuthorizedAccounts(enableAutoSignIn: Boolean)
    fun signInWithGoogle()
    fun signInWithApple()
}

@Inject
class CommonAuthViewModel(
    private val snackbarSender: SnackbarSender,
    private val bookRepo: BookRepo,
    private val tracker: Tracker,
    private val navController: NavController<BookDest>,
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
            snackbarSender.send(message)
        }

        val destination = try {
            if (bookRepo.isUserHasBooks()) {
                BookDest.Record
            } else {
                BookDest.Welcome
            }
        } catch (e: Exception) {
            snackbarSender.sendError(e)
            BookDest.Welcome
        } finally {
            isLoading.value = false
        }
        navController.selectRootAndClearAll(destination)
    }
}