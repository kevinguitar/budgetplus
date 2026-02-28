package com.kevlina.budgetplus.notification

import co.touchlab.kermit.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

interface TopicMessaging {
    fun subscribeToTopic(topic: String)
    fun unsubscribeFromTopic(topic: String)
}

@ContributesBinding(AppScope::class)
class FirebaseTopicMessaging : TopicMessaging {
    override fun subscribeToTopic(topic: String) {
        Firebase.messaging.subscribeToTopic(topic)
        Logger.d { "Notification: Subscribed to $topic topic" }
    }

    override fun unsubscribeFromTopic(topic: String) {
        Firebase.messaging.unsubscribeFromTopic(topic)
        Logger.d { "Notification: Subscribed to $topic topic" }
    }
}

