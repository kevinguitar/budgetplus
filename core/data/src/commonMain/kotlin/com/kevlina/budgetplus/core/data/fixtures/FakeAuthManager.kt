package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.VisibleForTesting
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.remote.User
import kotlinx.coroutines.flow.MutableStateFlow

@VisibleForTesting
class FakeAuthManager(
    user: User? = null,
    isPremium: Boolean = false,
    override val userId: String? = user?.id
) : AuthManager {

    override val userState = MutableStateFlow(user)
    override val isPremium = MutableStateFlow(isPremium)

    override fun requireUserId(): String = requireNotNull(userId) { "User id is null" }

    override suspend fun renameUser(newName: String) {
        error("Not yet implemented")
    }

    override suspend fun markPremium(isPremium: Boolean) {
        error("Not yet implemented")
    }

    override fun updateFcmToken(newToken: String) {
        error("Not yet implemented")
    }

    override suspend fun logout() {
        error("Not yet implemented")
    }
}