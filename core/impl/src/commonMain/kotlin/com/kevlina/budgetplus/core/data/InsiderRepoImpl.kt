package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.data.remote.UsersDb
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Direction
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlin.time.Clock
import kotlin.time.Duration

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class InsiderRepoImpl(
    @UsersDb private val usersDb: Lazy<CollectionReference>,
) : InsiderRepo {

    override suspend fun getTotalUsers(): Long {
        return usersDb.value.count()
    }

    override suspend fun getTotalPremiumUsers(): Long {
        return usersDb.value
            .where { "premium" equalTo true }
            .count()
    }

    override suspend fun getTotalUsersByLanguage(language: String): Long {
        return usersDb.value
            .where { "language" equalTo language }
            .count()
    }

    override suspend fun getActiveUsers(duration: Duration): Long {
        val threshold = Clock.System.now().toEpochMilliseconds() - duration.inWholeMilliseconds
        return usersDb.value
            .where { "lastActiveOn" greaterThanOrEqualTo threshold }
            .count()
    }

    override suspend fun getNewUsers(count: Int): List<User> {
        return usersDb.value
            .orderBy("createdOn", Direction.DESCENDING)
            .limit(count)
            .get()
            .documents
            .map { it.data<User>() }
    }

    override suspend fun getActivePremiumUsers(count: Int): List<User> {
        return usersDb.value
            .where { "premium" equalTo true }
            .orderBy("lastActiveOn", Direction.DESCENDING)
            .limit(count)
            .get()
            .documents
            .map { it.data<User>() }
    }
}
