package com.kevlina.budgetplus.core.data

import co.touchlab.kermit.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class FcmTokenProviderImpl : FcmTokenProvider {

    override suspend fun getToken(): String? {
        return try {
            Firebase.messaging.getToken()
        } catch (e: Exception) {
            Logger.w(e) { "Failed to retrieve the fcm token" }
            null
        }
    }
}
