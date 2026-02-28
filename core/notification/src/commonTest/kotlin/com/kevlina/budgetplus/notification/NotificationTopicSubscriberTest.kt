package com.kevlina.budgetplus.notification

import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.remote.User
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationTopicSubscriberTest {

    @Test
    fun `initial subscription for free user`() = runTest {
        val user = User(id = "user1", language = "en", premium = false)
        authManager.userState.value = user

        createSubscriber().onAppStart()
        advanceUntilIdle()

        assertEquals(setOf("general_en", "free_user_en"), topicMessaging.subscriptions)
        assertTrue(topicMessaging.unsubscriptions.isEmpty())
    }

    @Test
    fun `initial subscription for premium user`() = runTest {
        val user = User(id = "user1", language = "zh-tw", premium = true)
        authManager.userState.value = user

        createSubscriber().onAppStart()
        advanceUntilIdle()

        assertEquals(setOf("general_tw", "premium_user_tw"), topicMessaging.subscriptions)
        assertTrue(topicMessaging.unsubscriptions.isEmpty())
    }

    @Test
    fun `avoid resubscription if info is the same`() = runTest {
        val subscriber = createSubscriber()
        val user = User(id = "user1", language = "en", premium = false)
        authManager.userState.value = user

        subscriber.onAppStart()
        advanceUntilIdle()

        topicMessaging.clear()

        // Call again with same info
        subscriber.onAppStart()
        advanceUntilIdle()

        assertTrue(topicMessaging.subscriptions.isEmpty())
        assertTrue(topicMessaging.unsubscriptions.isEmpty())
    }

    @Test
    fun `cleanup and resubscribe when language changes`() = runTest {
        val subscriber = createSubscriber()
        val userEn = User(id = "user1", language = "en", premium = false)
        authManager.userState.value = userEn

        subscriber.onAppStart()
        advanceUntilIdle()

        topicMessaging.clear()

        // Language changes
        val userJa = User(id = "user1", language = "ja", premium = false)
        authManager.userState.value = userJa

        // onAppStart would launch another subscribeToTopics in reality, but for testing we can call onAppStart again
        subscriber.onAppStart()
        advanceUntilIdle()

        assertEquals(setOf("general_en", "free_user_en"), topicMessaging.unsubscriptions)
        assertEquals(setOf("general_ja", "free_user_ja"), topicMessaging.subscriptions)
    }

    @Test
    fun `cleanup and resubscribe when premium status changes`() = runTest {
        val subscriber = createSubscriber()
        val freeUser = User(id = "user1", language = "en", premium = false)
        authManager.userState.value = freeUser

        subscriber.onAppStart()
        advanceUntilIdle()

        topicMessaging.clear()

        // Becomes premium
        val premiumUser = User(id = "user1", language = "en", premium = true)
        authManager.userState.value = premiumUser

        subscriber.onAppStart()
        advanceUntilIdle()

        assertEquals(setOf("general_en", "free_user_en"), topicMessaging.unsubscriptions)
        assertEquals(setOf("general_en", "premium_user_en"), topicMessaging.subscriptions)
    }

    @Test
    fun `no cleanup when user id changes`() = runTest {
        val subscriber = createSubscriber()
        val user1 = User(id = "user1", language = "en", premium = false)
        authManager.userState.value = user1

        subscriber.onAppStart()
        advanceUntilIdle()

        topicMessaging.clear()

        // Different user
        val user2 = User(id = "user2", language = "en", premium = false)
        authManager.userState.value = user2

        subscriber.onAppStart()
        advanceUntilIdle()

        assertTrue(topicMessaging.unsubscriptions.isEmpty())
        assertEquals(setOf("general_en", "free_user_en"), topicMessaging.subscriptions)
    }

    private val authManager = FakeAuthManager()
    private val preference = FakePreference()
    private val topicMessaging = FakeTopicMessaging()

    private fun TestScope.createSubscriber() = NotificationTopicSubscriber(
        authManager = authManager,
        preference = preference,
        topicMessaging = topicMessaging,
        appScope = this
    )

    private class FakeTopicMessaging : TopicMessaging {
        val subscriptions = mutableSetOf<String>()
        val unsubscriptions = mutableSetOf<String>()

        override fun subscribeToTopic(topic: String) {
            subscriptions.add(topic)
        }

        override fun unsubscribeFromTopic(topic: String) {
            unsubscriptions.add(topic)
        }

        fun clear() {
            subscriptions.clear()
            unsubscriptions.clear()
        }
    }
}
