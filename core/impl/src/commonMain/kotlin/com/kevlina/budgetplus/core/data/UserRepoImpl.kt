package com.kevlina.budgetplus.core.data

import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.data.remote.UsersDb
import dev.gitlive.firebase.firestore.CollectionReference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<UserRepo>())
@ContributesIntoSet(AppScope::class, binding = binding<AppStartAction>())
internal class UserRepoImpl(
    private val authManager: AuthManager,
    @UsersDb private val usersDb: Lazy<CollectionReference>,
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val bookRepo: BookRepo,
) : UserRepo, AppStartAction {

    private val userMapping = hashMapOf<String, User>()

    override fun onAppStart() {
        combine(
            bookRepo.bookState.filterNotNull(),
            authManager.userState.filterNotNull(),
            ::requestBookAuthors
        ).launchIn(appScope)
    }

    override fun getUser(userId: String): User? {
        val myUser = authManager.userState.value
        return if (userId == myUser?.id) {
            myUser
        } else {
            userMapping[userId]
        }
    }

    // Request the other user only once per app life, and do not request own user to save resource.
    private suspend fun requestBookAuthors(book: Book, myUser: User) {
        val authorIdsToRequest = book.authors.filter { authorId ->
            authorId != myUser.id && authorId !in userMapping
        }

        if (authorIdsToRequest.isEmpty()) return

        coroutineScope {
            authorIdsToRequest
                .map { authorId -> async { loadUser(authorId) } }
                .awaitAll()
        }
    }

    private suspend fun loadUser(userId: String) {
        try {
            val user = usersDb.value.document(userId).get().data<User>()
            userMapping[userId] = user
        } catch (e: Exception) {
            Logger.e(e) { "UserRepo: loadUser failed" }
        }
    }
}