package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.common.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
internal class FcmTokenProviderImpl : FcmTokenProvider {

    override suspend fun getToken(): String? {
        return try {
            Firebase.messaging.getToken()
        } catch (e: Exception) {
            Logger.w(e, "Failed to retrieve the fcm token")
            null
        }
    }
}
