package com.kevlina.budgetplus.core.data

import androidx.datastore.preferences.core.stringPreferencesKey
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.app_language
import budgetplus.core.common.generated.resources.premium_unlocked
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.common.nav.NavigationAction
import com.kevlina.budgetplus.core.common.nav.NavigationFlow
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.data.remote.UsersDb
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.crashlytics.crashlytics
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.messaging.messaging
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AuthManagerImpl(
    private val preference: Preference,
    private val tracker: Lazy<Tracker>,
    @Named("allow_update_fcm_token") private val allowUpdateFcmToken: Boolean,
    @Named("auth") private val authNavigationAction: NavigationAction,
    private val navigationFlow: NavigationFlow,
    private val snackbarSender: SnackbarSender,
    @AppCoroutineScope private val appScope: CoroutineScope,
    @UsersDb private val usersDb: Lazy<CollectionReference>,
) : AuthManager {

    private val currentUserKey = stringPreferencesKey("currentUser")
    private val currentUserFlow = preference.of(currentUserKey, User.serializer())

    // Critical default value for app start
    override val userState: StateFlow<User?> = runBlocking {
        currentUserFlow
            .filterNotNull()
            .stateIn(
                scope = appScope,
                started = SharingStarted.Eagerly,
                initialValue = currentUserFlow.first()
            )
    }
    private val currentUser: User? get() = userState.value

    override val isPremium: StateFlow<Boolean> = userState.mapState { it?.premium == true }
    override val userId: String? get() = userState.value?.id

    init {
        appScope.launch { userState.collect() }

        // Quite terrible code but this is to skip the first null coming from Firebase Auth, we don't want to clear
        // the user preference (it is already null) and perform the navigation while the user already on auth screen.
        var hasSkippedFirstNull = false
        Firebase.auth
            .authStateChanged
            .onEach { firebaseUser ->
                when {
                    firebaseUser != null -> updateUser(firebaseUser.toUser())
                    !hasSkippedFirstNull -> Unit
                    else -> updateUser(null)
                }
                hasSkippedFirstNull = true
            }
            .launchIn(scope = appScope)
    }

    override fun requireUserId(): String {
        return requireNotNull(userId) { "User is null." }
    }

    override suspend fun renameUser(newName: String) {
        val currentUser = Firebase.auth.currentUser ?: error("Current user is null.")
        currentUser.updateProfile(displayName = newName)

        updateUser(
            user = currentUser.toUser().copy(name = newName),
            newName = newName
        )
        tracker.value.logEvent("user_renamed")
    }

    override suspend fun markPremium(isPremium: Boolean) {
        if (currentUser?.premium == isPremium) return

        val premiumUser = currentUser?.copy(premium = isPremium) ?: return
        usersDb.value.document(premiumUser.id).set(premiumUser)
        setUserToPreference(premiumUser)

        if (isPremium) {
            snackbarSender.send(Res.string.premium_unlocked)
        } else {
            //TODO: Off boarding the user when they lost the premium access.
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
            usersDb.value.document(userWithNewToken.id).set(userWithNewToken)
            setUserToPreference(userWithNewToken)
        }
    }

    override suspend fun logout() {
        tracker.value.logEvent("logout")
        Firebase.auth.signOut()
    }

    private fun FirebaseUser.toUser() = User(
        id = uid,
        name = displayName,
        photoUrl = photoURL,
    )

    private suspend fun updateUser(user: User?, newName: String? = null) {
        if (user == null) {
            setUserToPreference(null)
            return
        }

        // Associate the crash report with Budget+ user
        Firebase.crashlytics.setUserId(user.id)

        val userWithExclusiveFields = user.copy(
            premium = currentUser?.premium,
            createdOn = currentUser?.createdOn ?: Clock.System.now().toEpochMilliseconds(),
            lastActiveOn = Clock.System.now().toEpochMilliseconds(),
            language = getString(Res.string.app_language),
        )
        setUserToPreference(userWithExclusiveFields)

        val fcmToken = if (allowUpdateFcmToken) {
            try {
                Firebase.messaging.getToken()
            } catch (e: Exception) {
                Logger.w(e) { "Failed to retrieve the fcm token" }
                null
            }
        } else {
            null
        }
        Logger.d { "Fcm token: $fcmToken" }

        try {
            // Get the latest remote user from the server
            val remoteUserSnapshot = usersDb.value.document(user.id).get(Source.SERVER)
            if (remoteUserSnapshot.exists) {
                val remoteUser = remoteUserSnapshot.data<User>()
                // Merge exclusive fields to the Firebase auth user
                val mergedUser = userWithExclusiveFields.copy(
                    name = newName ?: remoteUser.name,
                    premium = remoteUser.premium,
                    internal = remoteUser.internal ?: false,
                    createdOn = remoteUser.createdOn,
                    fcmToken = fcmToken ?: remoteUser.fcmToken
                )
                setUserToPreference(mergedUser)

                usersDb.value.document(user.id).set(mergedUser)
            } else {
                Logger.i { "Can't find user in the db yet, set it with the data what we have in place." }
                usersDb.value.document(user.id)
                    .set(userWithExclusiveFields.copy(fcmToken = fcmToken))
            }
        } catch (e: Exception) {
            Logger.w(e) { "AuthManager update failed" }
        }
    }

    private suspend fun setUserToPreference(user: User?) {
        if (user == null) {
            preference.remove(currentUserKey)
            navigationFlow.sendEvent(authNavigationAction)
        } else {
            preference.update(currentUserKey, User.serializer(), user)
        }
    }
}