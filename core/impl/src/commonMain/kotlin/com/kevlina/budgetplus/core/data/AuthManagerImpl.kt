package com.kevlina.budgetplus.core.data

import androidx.datastore.preferences.core.stringPreferencesKey
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.premium_unlocked
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.data.remote.User
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class AuthManagerImpl(
    private val preference: Preference,
    private val tracker: Lazy<Tracker>,
    @Named("allow_update_fcm_token") private val allowUpdateFcmToken: Boolean,
    private val logoutNavigation: LogoutNavigation,
    private val snackbarSender: SnackbarSender,
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val authState: AuthState,
    private val userDbClient: UserDbClient,
    private val fcmTokenProvider: FcmTokenProvider,
    private val crashlyticsProvider: CrashlyticsProvider,
    private val cloudFunctionsCaller: CloudFunctionsCaller,
    private val appLanguageProvider: AppLanguageProvider,
) : AuthManager {

    private val currentUserKey = stringPreferencesKey("currentUser")
    private val currentUserFlow = preference.of(currentUserKey, User.serializer())

    override val userState: StateFlow<User?> = currentUserFlow.stateIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        // Critical default value for app start
        initialValue = runBlocking { currentUserFlow.first() }
    )
    private val currentUser: User? get() = userState.value

    override val isPremium: StateFlow<Boolean> = userState.mapState { it?.premium == true }
    override val userId: String? get() = userState.value?.id

    init {
        authState
            .authStateChanged
            .onEach(::updateUser)
            .launchIn(appScope)
    }

    override fun requireUserId(): String {
        return requireNotNull(userId) { "User is null." }
    }

    override suspend fun renameUser(newName: String) {
        val user = currentUser ?: error("Current user is null.")
        authState.updateCurrentUserProfile(displayName = newName)
        updateUser(
            user = user.copy(name = newName),
            newName = newName
        )
        tracker.value.logEvent("user_renamed")
    }

    override suspend fun markPremium(isPremium: Boolean) {
        if (currentUser?.premium == isPremium) return

        val premiumUser = currentUser?.copy(premium = isPremium) ?: return
        try {
            userDbClient.setUser(premiumUser)
            setUserToPreference(premiumUser)

            if (isPremium) {
                tracker.value.logEvent("buy_premium_success")
                snackbarSender.send(Res.string.premium_unlocked)
            }
        } catch (e: Exception) {
            snackbarSender.sendError(e)
        }
    }

    override fun updateFcmToken(newToken: String) {
        Logger.d { "New fcm token: $newToken" }
        if (!allowUpdateFcmToken) return
        if (currentUser?.fcmToken == newToken) {
            Logger.d { "Fcm token is the same, skip the update." }
            return
        }

        val userWithNewToken = currentUser?.copy(fcmToken = newToken) ?: return
        appScope.launch {
            try {
                userDbClient.setUser(userWithNewToken)
                setUserToPreference(userWithNewToken)
            } catch (e: Exception) {
                Logger.w(e) { "Failed to update fcm token" }
            }
        }
    }

    override suspend fun logout() {
        tracker.value.logEvent("logout")
        authState.signOut()
    }

    override fun deleteUserAccount(): Job = appScope.launch {
        tracker.value.logEvent("delete_account")
        try {
            val data = mapOf("userId" to userId)
            cloudFunctionsCaller.call(
                functionName = "deleteUserAccount",
                region = "asia-southeast1",
                data = data
            )

            logout()
        } catch (e: Exception) {
            snackbarSender.sendError(e)
        }
    }

    private suspend fun updateUser(user: User?, newName: String? = null) {
        if (user == null) {
            setUserToPreference(null)
            return
        }

        // Associate the crash report with Budget+ user
        crashlyticsProvider.setUserId(user.id)

        val userWithExclusiveFields = user.copy(
            premium = currentUser?.premium,
            createdOn = currentUser?.createdOn ?: Clock.System.now().toEpochMilliseconds(),
            lastActiveOn = Clock.System.now().toEpochMilliseconds(),
            language = appLanguageProvider.getLanguage(),
        )
        setUserToPreference(userWithExclusiveFields)

        val fcmToken = if (allowUpdateFcmToken) {
            fcmTokenProvider.getToken()
        } else {
            null
        }
        Logger.d { "Fcm token: $fcmToken" }

        try {
            // Get the latest remote user from the server
            val remoteUser = userDbClient.getUser(user.id)
            if (remoteUser != null) {
                // Merge exclusive fields to the Firebase auth user
                val mergedUser = userWithExclusiveFields.copy(
                    name = newName ?: remoteUser.name,
                    premium = remoteUser.premium,
                    internal = remoteUser.internal ?: false,
                    createdOn = remoteUser.createdOn,
                    fcmToken = fcmToken ?: remoteUser.fcmToken
                )
                setUserToPreference(mergedUser)

                userDbClient.setUser(mergedUser)
            } else {
                Logger.i { "Can't find user in the db yet, set it with the data what we have in place." }
                userDbClient.setUser(userWithExclusiveFields.copy(fcmToken = fcmToken))
            }
        } catch (e: Exception) {
            Logger.w(e) { "AuthManager update failed" }
        }
    }

    private suspend fun setUserToPreference(user: User?) {
        if (user == null) {
            preference.remove(currentUserKey)
            logoutNavigation.navigate()
        } else {
            preference.update(currentUserKey, User.serializer(), user)
        }
    }
}