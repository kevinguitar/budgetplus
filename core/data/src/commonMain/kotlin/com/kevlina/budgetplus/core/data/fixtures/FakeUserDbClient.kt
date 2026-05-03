package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.UserDbClient
import com.kevlina.budgetplus.core.data.remote.User

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeUserDbClient : UserDbClient {

    val users = mutableMapOf<String, User>()

    var setUserError: Exception? = null
    var getUserError: Exception? = null

    override suspend fun getUser(userId: String): User? {
        getUserError?.let { throw it }
        return users[userId]
    }

    override suspend fun setUser(user: User) {
        setUserError?.let { throw it }
        users[user.id] = user
    }
}
