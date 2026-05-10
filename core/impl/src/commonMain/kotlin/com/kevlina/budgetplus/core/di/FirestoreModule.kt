package com.kevlina.budgetplus.core.di

import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.remote.BooksDb
import com.kevlina.budgetplus.core.data.remote.JoinInfoDb
import com.kevlina.budgetplus.core.data.remote.PushNotificationsDb
import com.kevlina.budgetplus.core.data.remote.RecordsDb
import com.kevlina.budgetplus.core.data.remote.UsersDb
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.firestore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
interface FirestoreModule {

    @UsersDb
    @SingleIn(AppScope::class)
    @Provides
    fun provideUsersDb(): CollectionReference {
        return Firebase.firestore.collection("users")
    }

    @BooksDb
    @SingleIn(AppScope::class)
    @Provides
    fun provideBooksDb(): CollectionReference {
        return Firebase.firestore.collection("books")
    }

    @RecordsDb
    @Provides
    fun provideRecordsDb(bookRepo: BookRepo): CollectionReference {
        return Firebase.firestore.collection("books")
            .document(requireNotNull(bookRepo.currentBookId) { "Book id is null" })
            .collection("records")
    }

    @JoinInfoDb
    @SingleIn(AppScope::class)
    @Provides
    fun provideJoinInfoDb(): CollectionReference {
        return Firebase.firestore.collection("join_info")
    }

    @PushNotificationsDb
    @SingleIn(AppScope::class)
    @Provides
    fun providePushNotificationsDb(): CollectionReference {
        return Firebase.firestore.collection("push_notifications")
    }
}