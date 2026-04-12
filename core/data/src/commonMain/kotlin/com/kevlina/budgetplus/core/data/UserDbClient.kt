package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.data.remote.User

interface UserDbClient {

    /**
     *  Get the latest user from the server.
     *  @return the user if found, null if the document does not exist.
     */
    suspend fun getUser(userId: String): User?

    suspend fun setUser(user: User)
}
