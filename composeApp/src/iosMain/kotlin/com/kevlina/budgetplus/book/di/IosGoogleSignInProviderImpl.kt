package com.kevlina.budgetplus.book.di

import GoogleSignIn.GIDSignIn
import com.kevlina.budgetplus.feature.auth.IosGoogleSignInProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIApplication
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@ContributesBinding(AppScope::class)
class IosGoogleSignInProviderImpl : IosGoogleSignInProvider {

    override suspend fun signInWithGoogle(): IosGoogleSignInProvider.Result {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: error("No root view controller")
        return suspendCancellableCoroutine { cont ->
            GIDSignIn.sharedInstance.signInWithPresentingViewController(rootViewController) { result, error ->
                if (error != null) {
                    // Eh cannot find a way to access the constant, -5 is user-cancelled.
                    if (error.code.toInt() == -5) {
                        cont.cancel()
                    } else {
                        cont.resumeWithException(RuntimeException(error.localizedDescription))
                    }
                }

                val idToken = result?.user?.idToken?.tokenString
                val accessToken = result?.user?.accessToken?.tokenString
                if (idToken == null) {
                    cont.cancel()
                } else {
                    cont.resume(IosGoogleSignInProvider.Result(
                        idToken = idToken,
                        accessToken = accessToken
                    ))
                }
            }
        }
    }
}