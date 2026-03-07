package com.kevlina.budgetplus.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.di.ViewModelKey
import com.kevlina.budgetplus.core.common.di.ViewModelScope
import dev.zacsweers.metro.ContributesIntoMap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.ByteString.Companion.toByteString
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.cancellation.CancellationException

@ViewModelKey(AuthViewModel::class)
@ContributesIntoMap(ViewModelScope::class)
actual class AuthViewModel(
    actual val commonAuthViewModel: CommonAuthViewModel,
    private val iosGoogleSignInProvider: IosGoogleSignInProvider,
    private val snackbarSender: SnackbarSender,
) : ViewModel() {

    actual fun signInWithGoogle() {
        viewModelScope.launch {
            try {
                val result = iosGoogleSignInProvider.signInWithGoogle()
                commonAuthViewModel.proceedGoogleSignInWithIdToken(
                    idToken = result.idToken,
                    accessToken = result.accessToken
                )
            } catch (e: CancellationException) {
                Logger.d(e) { "Google sign in canceled" }
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    private var activeAppleSignIn: AppleSignInSession? = null

    @OptIn(BetaInteropApi::class)
    actual fun signInWithApple() {
        val rawNonce = generateNonce()
        val hashedNonce = sha256(rawNonce)

        val appleIDProvider = ASAuthorizationAppleIDProvider()
        val request = appleIDProvider.createRequest().apply {
            requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
            nonce = hashedNonce
        }

        val session = AppleSignInSession(
            rawNonce = rawNonce,
            coroutineScope = viewModelScope,
            commonAuthViewModel = commonAuthViewModel,
            snackbarSender = snackbarSender,
            onComplete = { activeAppleSignIn = null }
        )
        activeAppleSignIn = session

        val authorizationController = ASAuthorizationController(listOf(request)).apply {
            delegate = session
            presentationContextProvider = session
        }
        authorizationController.performRequests()
    }

    private class AppleSignInSession(
        private val rawNonce: String,
        private val coroutineScope: CoroutineScope,
        private val commonAuthViewModel: CommonAuthViewModel,
        private val snackbarSender: SnackbarSender,
        private val onComplete: () -> Unit,
    ) : NSObject(), ASAuthorizationControllerDelegateProtocol, ASAuthorizationControllerPresentationContextProvidingProtocol {

        override fun authorizationController(
            controller: ASAuthorizationController,
            didCompleteWithAuthorization: ASAuthorization,
        ) {
            val credential = didCompleteWithAuthorization.credential
                as? ASAuthorizationAppleIDCredential
            if (credential == null) {
                onComplete()
                return
            }

            val idTokenData = credential.identityToken
            if (idTokenData == null) {
                onComplete()
                return
            }

            val idToken = idTokenData.toByteString().toByteArray().decodeToString()

            coroutineScope.launch {
                try {
                    commonAuthViewModel.proceedAppleSignIn(idToken, rawNonce)
                } finally {
                    onComplete()
                }
            }
        }

        override fun authorizationController(
            controller: ASAuthorizationController,
            didCompleteWithError: platform.Foundation.NSError,
        ) {
            coroutineScope.launch {
                if (didCompleteWithError.domain == platform.AuthenticationServices.ASAuthorizationErrorDomain &&
                    didCompleteWithError.code == platform.AuthenticationServices.ASAuthorizationErrorCanceled
                ) {
                    Logger.d { "Apple sign-in canceled" }
                } else {
                    snackbarSender.send(didCompleteWithError.localizedDescription)
                }
                onComplete()
            }
        }

        override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
            return UIApplication.sharedApplication.keyWindow ?: UIWindow()
        }
    }

    private fun generateNonce(): String {
        val buffer = ByteArray(32)
        val status = buffer.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, buffer.size.convert(), pinned.addressOf(0))
        }
        if (status != errSecSuccess) error("Unable to generate random bytes: $status")
        return buffer.toHexString()
    }

    private fun sha256(input: String): String {
        val data = input.encodeToByteArray()
        val hash = ByteArray(CC_SHA256_DIGEST_LENGTH)

        if (data.isEmpty()) {
            return hash.toHexString()
        }

        data.usePinned { inputPinned ->
            hash.usePinned { hashPinned ->
                CC_SHA256(
                    inputPinned.addressOf(0),
                    data.size.toUInt(),
                    hashPinned.addressOf(0).reinterpret()
                )
            }
        }
        return hash.toHexString()
    }
}