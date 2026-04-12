package com.kevlina.budgetplus.core.data

/**
 *  Wraps Firebase Messaging token retrieval for testability.
 */
interface FcmTokenProvider {

    suspend fun getToken(): String?
}
