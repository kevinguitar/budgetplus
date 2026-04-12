package com.kevlina.budgetplus.core.data

interface FcmTokenProvider {

    suspend fun getToken(): String?
}
