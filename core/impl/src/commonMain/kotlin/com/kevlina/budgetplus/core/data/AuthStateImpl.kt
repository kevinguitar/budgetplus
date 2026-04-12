package com.kevlina.budgetplus.core.data

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@ContributesBinding(AppScope::class)
class AuthStateImpl : AuthState {

    override val authStateChanged: Flow<AuthStateUser?> =
        Firebase.auth.authStateChanged.map { firebaseUser ->
            firebaseUser?.let {
                AuthStateUser(
                    uid = it.uid,
                    displayName = it.displayName,
                    photoURL = it.photoURL,
                )
            }
        }

    override suspend fun signOut() {
        Firebase.auth.signOut()
    }

    override suspend fun updateCurrentUserProfile(displayName: String) {
        val currentUser = Firebase.auth.currentUser ?: error("Current user is null.")
        currentUser.updateProfile(displayName = displayName)
    }
}
