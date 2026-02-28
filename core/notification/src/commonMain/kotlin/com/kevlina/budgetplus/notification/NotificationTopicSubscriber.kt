package com.kevlina.budgetplus.notification

import androidx.datastore.preferences.core.stringPreferencesKey
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.local.Preference
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
    private val topicMessaging: TopicMessaging,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : AppStartAction {

    private val lastSubscribeInfoKey = stringPreferencesKey("lastSubscribeInfo")
    private val lastInfoFlow = preference.of(lastSubscribeInfoKey, SubscribeInfo.serializer())

    override fun onAppStart() {
        appScope.launch { subscribeToTopics() }
    }

    private suspend fun subscribeToTopics() {
        val user = authManager.userState
            .filterNotNull()
            .first()

        val isPremium = user.premium ?: false
        val lastInfo = lastInfoFlow.first()
        val currentInfo = SubscribeInfo(userId = user.id, language = user.language, premium = isPremium)
        if (lastInfo == currentInfo) {
            // Avoid resubscribing the same topics
            return
        }

        if (lastInfo != null && lastInfo.userId == user.id) {
            // Clean up stale subscriptions
            topicMessaging.unsubscribeFromTopic("general".toLocalizedTopicId(lastInfo.language))
            topicMessaging.unsubscribeFromTopic(
                (if (lastInfo.premium) "premium_user" else "free_user").toLocalizedTopicId(lastInfo.language)
            )
        }

        val generalTopic = "general".toLocalizedTopicId(user.language)
        val premiumUserTopic = "premium_user".toLocalizedTopicId(user.language)
        val freeUserTopic = "free_user".toLocalizedTopicId(user.language)

        topicMessaging.subscribeToTopic(generalTopic)

        if (user.premium == true) {
            topicMessaging.subscribeToTopic(premiumUserTopic)
        } else {
            topicMessaging.subscribeToTopic(freeUserTopic)
        }

        preference.update(
            key = lastSubscribeInfoKey,
            serializer = SubscribeInfo.serializer(),
            value = currentInfo
        )
    }

    private fun String.toLocalizedTopicId(language: String?): String =
        when (language) {
            "zh-tw" -> "${this}_tw"
            "zh-cn" -> "${this}_cn"
            "ja" -> "${this}_ja"
            else -> "${this}_en"
        }
}

@Serializable
data class SubscribeInfo(
    val userId: String,
    val language: String?,
    val premium: Boolean,
)