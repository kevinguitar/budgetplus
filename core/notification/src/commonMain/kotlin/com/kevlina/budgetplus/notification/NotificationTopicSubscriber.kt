package com.kevlina.budgetplus.notification

import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.local.Preference
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@ContributesIntoSet(AppScope::class)
class NotificationTopicSubscriber(
    private val authManager: AuthManager,
    private val preference: Preference,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : AppStartAction {

    private val lastSubscribeInfoKey = stringPreferencesKey("lastInfo")
    private val lastInfoFlow = preference.of(lastSubscribeInfoKey, SubscribeInfo.serializer())

    override fun onAppStart() {
        appScope.launch { subscribeToTopics() }
    }

    private suspend fun subscribeToTopics() {
        val user = authManager.userState
            .filterNotNull()
            .first()

        val generalTopic = when (user.language) {
            "zh-tw" -> "general_tw"
            "zh-cn" -> "general_cn"
            "ja" -> "general_ja"
            else -> "general_en"
        }
        val lastInfo = lastInfoFlow.first()
        if (lastInfo == SubscribeInfo(userId = user.id, topic = generalTopic)) {
            // Avoid resubscribing the same topic
            return
        }

        lastInfo?.let { info ->
            if (info.userId == user.id) {
                Firebase.messaging.unsubscribeFromTopic(info.topic)
            }
        }

        Firebase.messaging.subscribeToTopic(generalTopic)
        Logger.d { "Notification: Subscribed to $generalTopic topic" }
        preference.update(
            key = lastSubscribeInfoKey,
            serializer = SubscribeInfo.serializer(),
            value = SubscribeInfo(
                userId = user.id,
                topic = generalTopic
            )
        )
    }
}

@Serializable
data class SubscribeInfo(
    val userId: String,
    val topic: String,
)