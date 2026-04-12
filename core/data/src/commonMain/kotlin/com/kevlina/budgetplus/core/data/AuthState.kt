package com.kevlina.budgetplus.core.data

import kotlinx.coroutines.flow.Flow

/**
 *  Wraps Firebase Auth operations for testability.
 */
interface AuthState {

    val authStateChanged: Flow<AuthStateUser?>

    suspend fun signOut()

    suspend fun updateCurrentUserProfile(displayName: String)
}

data class AuthStateUser(
    val uid: String,
    val displayName: String?,
    val photoURL: String?,
)
