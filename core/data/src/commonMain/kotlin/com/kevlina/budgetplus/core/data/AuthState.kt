package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.data.remote.User
import kotlinx.coroutines.flow.Flow

interface AuthState {

    val authStateChanged: Flow<User?>

    suspend fun signOut()

    suspend fun updateCurrentUserProfile(displayName: String)
}