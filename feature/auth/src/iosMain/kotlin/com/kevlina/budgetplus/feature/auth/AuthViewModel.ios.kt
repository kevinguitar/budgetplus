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
import kotlinx.coroutines.launch
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
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
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

    @OptIn(BetaInteropApi::class)
    actual fun signInWithApple() {
        val rawNonce = generateNonce()
        val hashedNonce = sha256(rawNonce)

        val appleIDProvider = ASAuthorizationAppleIDProvider()
        val request = appleIDProvider.createRequest().apply {
            requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
            nonce = hashedNonce
        }

        val authorizationController = ASAuthorizationController(listOf(request)).apply {
            delegate = object : NSObject(), ASAuthorizationControllerDelegateProtocol {
                override fun authorizationController(
                    controller: ASAuthorizationController,
                    didCompleteWithAuthorization: ASAuthorization,
                ) {
                    val credential = didCompleteWithAuthorization.credential
                        as? ASAuthorizationAppleIDCredential ?: return
                    val idTokenData = credential.identityToken ?: return
                    val idToken = NSString.create(data = idTokenData, encoding = NSUTF8StringEncoding) ?: return

                    viewModelScope.launch {
                        commonAuthViewModel.proceedAppleSignIn(idToken.toString(), rawNonce)
                    }
                }

                override fun authorizationController(
                    controller: ASAuthorizationController,
                    didCompleteWithError: platform.Foundation.NSError,
                ) {
                    viewModelScope.launch {
                        snackbarSender.send(didCompleteWithError.localizedDescription)
                    }
                }
            }
            presentationContextProvider = object : NSObject(), ASAuthorizationControllerPresentationContextProvidingProtocol {
                override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor {
                    return UIApplication.sharedApplication.keyWindow ?: UIWindow()
                }
            }
        }
        authorizationController.performRequests()
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