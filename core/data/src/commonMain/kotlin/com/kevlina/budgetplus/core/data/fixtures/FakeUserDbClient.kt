package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.UserDbClient
import com.kevlina.budgetplus.core.data.remote.User
import kotlinx.coroutines.CompletableDeferred

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeUserDbClient : UserDbClient {

    val users = mutableMapOf<String, User>()

    var setUserError: Exception? = null
    var getUserError: Exception? = null

    /**
     * When set, [getUser] suspends until this deferred completes. Lets a test hold [getUser]
     * mid-flight to deterministically interleave another operation (e.g. markPremium) that races
     * with [com.kevlina.budgetplus.core.data.AuthManager] user updates.
     */
    var getUserGate: CompletableDeferred<Unit>? = null

    override suspend fun getUser(userId: String): User? {
        // Snapshot before awaiting the gate so an operation that runs while this is parked (and
        // mutates the map via setUser) doesn't leak into the "remote" value returned here.
        val snapshot = users[userId]
        getUserGate?.await()
        getUserError?.let { throw it }
        return snapshot
    }

    override suspend fun setUser(user: User) {
        setUserError?.let { throw it }
        users[user.id] = user
    }
}
