package com.kevlina.budgetplus.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.kevlina.budgetplus.core.common.ActivityProvider
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.data.local.Preference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@ViewModelKey
@ContributesIntoMap(AppScope::class)
actual class AuthViewModel(
    actual val commonAuthViewModel: CommonAuthViewModel,
    private val activityProvider: ActivityProvider,
    private val snackbarSender: SnackbarSender,
    private val context: Context,
    referrerHandlerFactory: ReferrerHandler.Factory,
    preference: Preference,
) : ViewModel() {

    private val credentialManager by lazy { CredentialManager.create(context) }
    private val googleClientId get() = context.getString(R.string.google_cloud_client_id)

    private val isFirstLaunchAfterInstallKey = booleanPreferencesKey("isFirstLaunchAfterInstall")

    init {
        viewModelScope.launch {
            if (preference.of(isFirstLaunchAfterInstallKey).first() == null) {
                preference.update(isFirstLaunchAfterInstallKey, false)
                val currentActivity = activityProvider.currentActivity ?: return@launch
                referrerHandlerFactory.create(currentActivity).retrieveReferrer()
            }
        }
    }

    actual fun signInWithGoogle() {
        val siwgOption = GetSignInWithGoogleOption
            .Builder(googleClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(siwgOption)
            .build()

        viewModelScope.launch {
            try {
                val activity = activityProvider.activityFlow.filterNotNull().first()
                val result = credentialManager.getCredential(activity, request)
                handleSignIn(result)
            } catch (e: GetCredentialCancellationException) {
                // Ignore cancellation exception
                Logger.d(e) { "Google sign in canceled" }
            } catch (e: GetCredentialException) {
                snackbarSender.sendError(e)
            }
        }
    }

    // Not supported yet
    actual fun signInWithApple() = Unit

    /**
     *  If there are any accounts that were authorized before, launch the sign in dialog.
     */
    actual fun checkAuthorizedAccounts(enableAutoSignIn: Boolean) {
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(enableAutoSignIn)
            .setServerClientId(googleClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val activity = activityProvider.activityFlow.filterNotNull().first()
                val result = credentialManager.getCredential(activity, request)
                handleSignIn(result)
            } catch (e: GetCredentialCancellationException) {
                // Ignore cancellation exception
                Logger.d(e) { "Google sign in canceled" }
            } catch (e: NoCredentialException) {
                Logger.w(e) { "No credential is found" }
            } catch (e: GetCredentialException) {
                Logger.e(e) { "Fail to get credential" }
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        if (
            credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            viewModelScope.launch { snackbarSender.send("Unexpected type of credential") }
            Logger.e { "Unexpected type of credential. ${credential.type}" }
            return
        }

        val googleIdToken = try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (e: GoogleIdTokenParsingException) {
            snackbarSender.sendError(e)
            return
        }

        viewModelScope.launch {
            commonAuthViewModel.proceedGoogleSignInWithIdToken(googleIdToken, accessToken = null)
        }
    }
}

actual val isAppleSignInAvailable: Boolean get() = false