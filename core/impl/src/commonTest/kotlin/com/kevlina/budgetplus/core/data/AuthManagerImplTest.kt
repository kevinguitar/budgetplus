package com.kevlina.budgetplus.core.data

import androidx.datastore.preferences.core.stringPreferencesKey
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.data.fixtures.FakeAppLanguageProvider
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthState
import com.kevlina.budgetplus.core.data.fixtures.FakeCloudFunctionsCaller
import com.kevlina.budgetplus.core.data.fixtures.FakeCrashlyticsProvider
import com.kevlina.budgetplus.core.data.fixtures.FakeFcmTokenProvider
import com.kevlina.budgetplus.core.data.fixtures.FakeLogoutNavigation
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.fixtures.FakeUserDbClient
import com.kevlina.budgetplus.core.data.remote.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthManagerImplTest {

    @Test
    fun `userState emits null when no user stored`() = runTest {
        val manager = createAuthManager()
        assertNull(manager.userState.value)
    }

    @Test
    fun `userId returns null when no user stored`() = runTest {
        val manager = createAuthManager()
        assertNull(manager.userId)
    }

    @Test
    fun `isPremium is false when no user stored`() = runTest {
        val manager = createAuthManager()
        assertFalse(manager.isPremium.value)
    }

    @Test
    fun `requireUserId throws when user is null`() = runTest {
        val manager = createAuthManager()
        assertFailsWith<IllegalArgumentException> {
            manager.requireUserId()
        }
    }

    @Test
    fun `requireUserId returns userId when user exists`() = runTest {
        val preference = FakePreference()
        val manager = createAuthManager(preference = preference)
        preference.setUser(User(id = "user123", name = "Alice"))
        assertEquals("user123", manager.userState.first { it != null }?.id)
        assertEquals("user123", manager.requireUserId())
    }

    // -- markPremium tests --

    @Test
    fun `markPremium does nothing when already same premium state`() = runTest {
        val preference = FakePreference()
        val userDbClient = FakeUserDbClient()
        val manager = createAuthManager(preference = preference, userDbClient = userDbClient)
        preference.setUser(User(id = "user1", premium = true))
        manager.userState.first { it != null }

        manager.markPremium(isPremium = true)

        // Should not have updated the DB
        assertFalse(userDbClient.users.containsKey("user1"))
    }

    @Test
    fun `markPremium updates user to premium in DB and preference`() = runTest {
        val preference = FakePreference()
        val userDbClient = FakeUserDbClient()
        val snackbarSender = FakeSnackbarSender
        snackbarSender.lastSentMessageRes = null

        val manager = createAuthManager(
            preference = preference,
            userDbClient = userDbClient,
            snackbarSender = snackbarSender,
        )
        preference.setUser(User(id = "user1", name = "Alice", premium = false))
        manager.userState.first { it != null }

        manager.markPremium(isPremium = true)

        // User should be stored in DB with premium = true
        assertEquals(true, userDbClient.users["user1"]?.premium)
        // User in preference should be premium
        assertEquals(true, manager.userState.value?.premium)
        // Snackbar should have been sent
        assertNotNull(snackbarSender.lastSentMessageRes)
    }

    @Test
    fun `markPremium to false does not send snackbar`() = runTest {
        val preference = FakePreference()
        val userDbClient = FakeUserDbClient()
        val snackbarSender = FakeSnackbarSender
        snackbarSender.lastSentMessageRes = null

        val manager = createAuthManager(
            preference = preference,
            userDbClient = userDbClient,
            snackbarSender = snackbarSender,
        )
        preference.setUser(User(id = "user1", name = "Alice", premium = true))
        manager.userState.first { it != null }

        manager.markPremium(isPremium = false)

        assertEquals(false, userDbClient.users["user1"]?.premium)
        assertNull(snackbarSender.lastSentMessageRes)
    }

    @Test
    fun `markPremium sends error on DB failure`() = runTest {
        val preference = FakePreference()
        val userDbClient = FakeUserDbClient()
        val testError = RuntimeException("DB error")
        userDbClient.setUserError = testError
        val snackbarSender = FakeSnackbarSender
        snackbarSender.lastSentError = null

        val manager = createAuthManager(
            preference = preference,
            userDbClient = userDbClient,
            snackbarSender = snackbarSender,
        )
        preference.setUser(User(id = "user1", name = "Alice", premium = false))
        manager.userState.first { it != null }

        manager.markPremium(isPremium = true)

        assertEquals(testError, snackbarSender.lastSentError)
    }

    @Test
    fun `markPremium does nothing when current user is null`() = runTest {
        val userDbClient = FakeUserDbClient()
        val manager = createAuthManager(userDbClient = userDbClient)

        manager.markPremium(isPremium = true)

        assertTrue(userDbClient.users.isEmpty())
    }

    // -- updateFcmToken tests --

    @Test
    fun `updateFcmToken does nothing when allowUpdateFcmToken is false`() = runTest {
        val preference = FakePreference()
        val userDbClient = FakeUserDbClient()
        val manager = createAuthManager(
            preference = preference,
            userDbClient = userDbClient,
            allowUpdateFcmToken = false,
        )
        preference.setUser(User(id = "user1", name = "Alice"))
        manager.userState.first { it != null }

        manager.updateFcmToken("new_token")

        assertTrue(userDbClient.users.isEmpty())
    }

    @Test
    fun `updateFcmToken does nothing when token is same as current`() = runTest {
        val preference = FakePreference()
        val userDbClient = FakeUserDbClient()
        val manager = createAuthManager(preference = preference, userDbClient = userDbClient)
        preference.setUser(User(id = "user1", name = "Alice", fcmToken = "same_token"))
        manager.userState.first { it != null }

        manager.updateFcmToken("same_token")

        assertTrue(userDbClient.users.isEmpty())
    }

    @Test
    fun `updateFcmToken does nothing when current user is null`() = runTest {
        val userDbClient = FakeUserDbClient()
        val manager = createAuthManager(userDbClient = userDbClient)

        manager.updateFcmToken("new_token")

        assertTrue(userDbClient.users.isEmpty())
    }

    @Test
    fun `updateFcmToken updates token in DB and preference`() = runTest {
        val preference = FakePreference()
        val userDbClient = FakeUserDbClient()
        val manager = createAuthManager(preference = preference, userDbClient = userDbClient)
        preference.setUser(User(id = "user1", name = "Alice", fcmToken = "old_token"))
        manager.userState.first { it != null }

        manager.updateFcmToken("new_token")
        // Wait for the launched coroutine to complete
        testScheduler.advanceUntilIdle()

        assertEquals("new_token", userDbClient.users["user1"]?.fcmToken)
        assertEquals("new_token", manager.userState.value?.fcmToken)
    }

    // -- logout tests --

    @Test
    fun `logout signs out and tracks event`() = runTest {
        val authState = FakeAuthState()
        val tracker = FakeTracker()
        val manager = createAuthManager(authState = authState, tracker = tracker)

        manager.logout()

        assertEquals("logout", tracker.lastEventName)
        assertTrue(authState.signedOut)
    }

    // -- deleteUserAccount tests --

    @Test
    fun `deleteUserAccount calls cloud function and logs out`() = runTest {
        val preference = FakePreference()
        val authState = FakeAuthState()
        val cloudFunctionsCaller = FakeCloudFunctionsCaller()
        val tracker = FakeTracker()
        val manager = createAuthManager(
            preference = preference,
            authState = authState,
            cloudFunctionsCaller = cloudFunctionsCaller,
            tracker = tracker,
        )
        preference.setUser(User(id = "user1", name = "Alice"))
        manager.userState.first { it != null }

        manager.deleteUserAccount()
        testScheduler.advanceUntilIdle()

        // deleteUserAccount calls logout() at the end, which sets lastEvent to "logout"
        assertEquals("logout", tracker.lastEventName)
        assertNotNull(cloudFunctionsCaller.lastCall)
        assertEquals("deleteUserAccount", cloudFunctionsCaller.lastCall?.first)
        assertEquals("asia-southeast1", cloudFunctionsCaller.lastCall?.second)
        assertTrue(authState.signedOut)
    }

    @Test
    fun `deleteUserAccount sends error on failure`() = runTest {
        val cloudFunctionsCaller = FakeCloudFunctionsCaller()
        val testError = RuntimeException("Function error")
        cloudFunctionsCaller.callError = testError
        val snackbarSender = FakeSnackbarSender
        snackbarSender.lastSentError = null

        val manager = createAuthManager(
            cloudFunctionsCaller = cloudFunctionsCaller,
            snackbarSender = snackbarSender,
        )

        manager.deleteUserAccount()
        testScheduler.advanceUntilIdle()

        assertEquals(testError, snackbarSender.lastSentError)
    }

    // -- renameUser tests --

    @Test
    fun `renameUser updates profile and tracks event`() = runTest {
        val preference = FakePreference()
        val authState = FakeAuthState()
        val tracker = FakeTracker()
        val userDbClient = FakeUserDbClient()
        val manager = createAuthManager(
            preference = preference,
            authState = authState,
            tracker = tracker,
            userDbClient = userDbClient,
        )
        preference.setUser(User(id = "user1", name = "Alice"))
        manager.userState.first { it != null }

        manager.renameUser("Bob")

        assertEquals("Bob", authState.lastUpdatedDisplayName)
        assertEquals("user_renamed", tracker.lastEventName)
    }

    // -- auth state changed tests --

    @Test
    fun `auth state change with null user triggers logout navigation`() = runTest {
        val preference = FakePreference()
        val authState = FakeAuthState()
        val logoutNavigation = FakeLogoutNavigation()
        val manager = createAuthManager(
            preference = preference,
            authState = authState,
            logoutNavigation = logoutNavigation,
        )
        // Set up a user first
        preference.setUser(User(id = "user1", name = "Alice"))
        manager.userState.first { it != null }

        // Emit null auth state (user signed out)
        authState.authStateFlow.emit(null)
        testScheduler.advanceUntilIdle()

        assertTrue(logoutNavigation.navigated)
        assertNull(manager.userState.value)
    }

    @Test
    fun `auth state change with user updates crashlytics and preference`() = runTest {
        val authState = FakeAuthState()
        val crashlyticsProvider = FakeCrashlyticsProvider()
        val userDbClient = FakeUserDbClient()
        val manager = createAuthManager(
            authState = authState,
            crashlyticsProvider = crashlyticsProvider,
            userDbClient = userDbClient,
        )

        // Emit auth state with a new user
        authState.authStateFlow.emit(
            User(id = "user1", name = "Alice", photoUrl = "photo.jpg")
        )
        testScheduler.advanceUntilIdle()

        assertEquals("user1", crashlyticsProvider.lastUserId)
        // User should be stored in preference
        assertNotNull(manager.userState.value)
        assertEquals("user1", manager.userState.value?.id)
    }

    @Test
    fun `auth state change merges with existing remote user`() = runTest {
        val authState = FakeAuthState()
        val userDbClient = FakeUserDbClient()
        // Pre-populate remote user with premium and custom name
        userDbClient.users["user1"] = User(
            id = "user1",
            name = "RemoteName",
            premium = true,
            internal = true,
            createdOn = 1000L,
            fcmToken = "remote_token"
        )
        val fcmTokenProvider = FakeFcmTokenProvider(token = null)
        val manager = createAuthManager(
            authState = authState,
            userDbClient = userDbClient,
            fcmTokenProvider = fcmTokenProvider,
        )

        // Emit auth state
        authState.authStateFlow.emit(
            User(id = "user1", name = "AuthName")
        )
        testScheduler.advanceUntilIdle()

        // Merged user should use remote's name, premium, internal, createdOn, fcmToken
        val storedUser = manager.userState.value
        assertNotNull(storedUser)
        assertEquals("RemoteName", storedUser.name)
        assertEquals(true, storedUser.premium)
        assertEquals(true, storedUser.internal)
        assertEquals(1000L, storedUser.createdOn)
        assertEquals("remote_token", storedUser.fcmToken)
    }

    @Test
    fun `auth state change uses fcm token from provider when available`() = runTest {
        val authState = FakeAuthState()
        val userDbClient = FakeUserDbClient()
        userDbClient.users["user1"] = User(
            id = "user1",
            name = "Alice",
            fcmToken = "old_remote_token"
        )
        val fcmTokenProvider = FakeFcmTokenProvider(token = "new_local_token")
        val manager = createAuthManager(
            authState = authState,
            userDbClient = userDbClient,
            fcmTokenProvider = fcmTokenProvider,
        )

        authState.authStateFlow.emit(
            User(id = "user1", name = "Alice")
        )
        testScheduler.advanceUntilIdle()

        // FCM token from provider should take precedence over remote
        val storedUser = userDbClient.users["user1"]
        assertNotNull(storedUser)
        assertEquals("new_local_token", storedUser.fcmToken)
    }

    @Test
    fun `auth state change creates new user in DB when not found remotely`() = runTest {
        val authState = FakeAuthState()
        val userDbClient = FakeUserDbClient()
        val fcmTokenProvider = FakeFcmTokenProvider(token = "my_token")
        createAuthManager(
            authState = authState,
            userDbClient = userDbClient,
            fcmTokenProvider = fcmTokenProvider,
        )

        authState.authStateFlow.emit(
            User(id = "new_user", name = "NewUser")
        )
        testScheduler.advanceUntilIdle()

        // User should be created in DB
        val dbUser = userDbClient.users["new_user"]
        assertNotNull(dbUser)
        assertEquals("NewUser", dbUser.name)
        assertEquals("my_token", dbUser.fcmToken)
    }

    @Test
    fun `auth state change skips fcm token when allowUpdateFcmToken is false`() = runTest {
        val authState = FakeAuthState()
        val userDbClient = FakeUserDbClient()
        val fcmTokenProvider = FakeFcmTokenProvider(token = "should_not_be_used")
        createAuthManager(
            authState = authState,
            userDbClient = userDbClient,
            fcmTokenProvider = fcmTokenProvider,
            allowUpdateFcmToken = false,
        )

        authState.authStateFlow.emit(
            User(id = "user1", name = "Alice")
        )
        testScheduler.advanceUntilIdle()

        val dbUser = userDbClient.users["user1"]
        assertNotNull(dbUser)
        assertNull(dbUser.fcmToken)
    }

    @Test
    fun `auth state change handles DB failure gracefully`() = runTest {
        val authState = FakeAuthState()
        val userDbClient = FakeUserDbClient()
        userDbClient.getUserError = RuntimeException("DB unavailable")
        val manager = createAuthManager(
            authState = authState,
            userDbClient = userDbClient,
        )

        authState.authStateFlow.emit(
            User(id = "user1", name = "Alice")
        )
        testScheduler.advanceUntilIdle()

        // User should still be stored in preference (set before DB call)
        assertNotNull(manager.userState.value)
        assertEquals("user1", manager.userState.value?.id)
    }

    // -- isPremium tests --

    @Test
    fun `isPremium reflects user premium state`() = runTest {
        val preference = FakePreference()
        val manager = createAuthManager(preference = preference)

        assertFalse(manager.isPremium.value)

        preference.setUser(User(id = "user1", premium = true))
        manager.userState.first { it?.premium == true }
        assertTrue(manager.isPremium.value)

        preference.setUser(User(id = "user1", premium = false))
        manager.userState.first { it?.premium != true }
        assertFalse(manager.isPremium.value)
    }

    // -- Test setup --

    private fun TestScope.createAuthManager(
        preference: FakePreference = FakePreference(),
        authState: FakeAuthState = FakeAuthState(),
        tracker: FakeTracker = FakeTracker(),
        userDbClient: FakeUserDbClient = FakeUserDbClient(),
        fcmTokenProvider: FakeFcmTokenProvider = FakeFcmTokenProvider(),
        crashlyticsProvider: FakeCrashlyticsProvider = FakeCrashlyticsProvider(),
        cloudFunctionsCaller: FakeCloudFunctionsCaller = FakeCloudFunctionsCaller(),
        logoutNavigation: FakeLogoutNavigation = FakeLogoutNavigation(),
        snackbarSender: FakeSnackbarSender = FakeSnackbarSender,
        allowUpdateFcmToken: Boolean = true,
    ) = AuthManagerImpl(
        preference = preference,
        tracker = lazy { tracker },
        allowUpdateFcmToken = allowUpdateFcmToken,
        logoutNavigation = logoutNavigation,
        snackbarSender = snackbarSender,
        appScope = backgroundScope + UnconfinedTestDispatcher(testScheduler),
        authState = authState,
        userDbClient = userDbClient,
        fcmTokenProvider = fcmTokenProvider,
        crashlyticsProvider = crashlyticsProvider,
        cloudFunctionsCaller = cloudFunctionsCaller,
        appLanguageProvider = FakeAppLanguageProvider(),
    )
}

private suspend fun FakePreference.setUser(user: User) {
    update(
        key = stringPreferencesKey("currentUser"),
        serializer = User.serializer(),
        value = user,
    )
}
