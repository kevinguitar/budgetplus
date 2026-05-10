package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.AuthState
import com.kevlina.budgetplus.core.data.remote.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeAuthState : AuthState {

    val authStateFlow = MutableSharedFlow<User?>(replay = 1)

    var signedOut = false
        private set

    var lastUpdatedDisplayName: String? = null
        private set

    override val authStateChanged: Flow<User?> = authStateFlow

    override suspend fun signOut() {
        signedOut = true
    }

    override suspend fun updateCurrentUserProfile(displayName: String) {
        lastUpdatedDisplayName = displayName
    }
}
