package com.kevlina.budgetplus.core.data

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Query
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.data.remote.UsersDb
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.android
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.tasks.await
import kotlin.time.Clock
import kotlin.time.Duration

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class InsiderRepoImpl(
    @UsersDb private val usersDbKmp: Lazy<CollectionReference>,
) : InsiderRepo {

    // See https://github.com/GitLiveApp/firebase-kotlin-sdk/issues/657
    private val usersDb get() = usersDbKmp.value.android

    override suspend fun getTotalUsers(): Long {
        return usersDb
            .count()
            .get(AggregateSource.SERVER)
            .await()
            .count
    }

    override suspend fun getTotalPremiumUsers(): Long {
        return usersDb
            .whereEqualTo("premium", true)
            .count()
            .get(AggregateSource.SERVER)
            .await()
            .count
    }

    override suspend fun getTotalUsersByLanguage(language: String): Long {
        return usersDb
            .whereEqualTo("language", language)
            .count()
            .get(AggregateSource.SERVER)
            .await()
            .count
    }

    override suspend fun getActiveUsers(duration: Duration): Long {
        val threshold = Clock.System.now().toEpochMilliseconds() - duration.inWholeMilliseconds
        return usersDb
            .whereGreaterThanOrEqualTo("lastActiveOn", threshold)
            .count()
            .get(AggregateSource.SERVER)
            .await()
            .count
    }

    override suspend fun getNewUsers(count: Int): List<User> {
        return usersDb
            .orderBy("createdOn", Query.Direction.DESCENDING)
            .limit(count.toLong())
            .get()
            .await()
            .toObjects(User::class.java)
    }

    override suspend fun getActivePremiumUsers(count: Int): List<User> {
        return usersDb
            // Exclude internal users
            .whereEqualTo("internal", false)
            .whereEqualTo("premium", true)
            .orderBy("lastActiveOn", Query.Direction.DESCENDING)
            .limit(count.toLong())
            .get()
            .await()
            .toObjects(User::class.java)
    }
}