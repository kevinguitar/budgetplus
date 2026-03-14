package com.kevlina.budgetplus.core.data

import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.User
import dev.gitlive.firebase.firestore.CollectionReference
import io.mockk.mockk
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

internal class BookRepoImplTest {

    @Test
    fun `canEdit is true when Book#allowMembersEdit is null`() = runTest {
        val repo = createRepo(book = Book(allowMembersEdit = null))
        repo.bookState.filterNotNull().first()
        assertThat(repo.canEdit).isTrue()
    }

    @Test
    fun `canEdit is false when Book#allowMembersEdit is false and you're not the owner`() = runTest {
        val repo = createRepo(
            book = Book(
                ownerId = "not_my_user",
                allowMembersEdit = false
            )
        )
        repo.bookState.filterNotNull().first()
        assertThat(repo.canEdit).isFalse()
    }

    private val tracker = FakeTracker()

    private fun TestScope.createRepo(book: Book?) = BookRepoImpl(
        authManager = FakeAuthManager(user = User(id = "my_user")),
        joinInfoProcessor = mockk(),
        tracker = tracker,
        preference = FakePreference {
            set(stringPreferencesKey("currentBook"), Json.encodeToString(book))
        },
        appScope = backgroundScope,
        booksDb = lazy { mockk<CollectionReference>(relaxed = true) }
    )
}