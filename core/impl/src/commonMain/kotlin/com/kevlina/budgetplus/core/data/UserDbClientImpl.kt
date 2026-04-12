package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.data.remote.UsersDb
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Source
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
class UserDbClientImpl(
    @UsersDb private val usersDb: Lazy<CollectionReference>,
) : UserDbClient {

    override suspend fun getUser(userId: String): User? {
        val snapshot = usersDb.value.document(userId).get(Source.SERVER)
        return if (snapshot.exists) {
            snapshot.data<User>()
        } else {
            null
        }
    }

    override suspend fun setUser(user: User) {
        usersDb.value.document(user.id).set(user)
    }
}
