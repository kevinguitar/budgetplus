package com.kevlina.budgetplus.feature.freeze

import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.unit.test.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FreezeBookViewModelTest {

    @get:Rule
    val rule = MainDispatcherRule()

    private val activatedBookIdKey = stringPreferencesKey("activatedBookIdKey")

    @Test
    fun `showFreezeDialog is true when user is not premium and has multiple books`() = runTest {
        val authManager = FakeAuthManager(user = User(premium = false))
        val bookRepo = FakeBookRepo(books = listOf(Book(id = "1"), Book(id = "2")))
        val model = createViewModel(authManager, bookRepo)

        model.showFreezeDialog.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `showFreezeDialog is false when user is premium`() = runTest {
        val authManager = FakeAuthManager(user = User(premium = true))
        val bookRepo = FakeBookRepo(books = listOf(Book(id = "1"), Book(id = "2")))
        val model = createViewModel(authManager, bookRepo)

        model.showFreezeDialog.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `showFreezeDialog is false when user has only one book`() = runTest {
        val authManager = FakeAuthManager(user = User(premium = false))
        val bookRepo = FakeBookRepo(books = listOf(Book(id = "1")))
        val model = createViewModel(authManager, bookRepo)

        model.showFreezeDialog.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `showFreezeDialog is false when user has already activated a book`() = runTest {
        val authManager = FakeAuthManager(user = User(premium = false))
        val bookRepo = FakeBookRepo(books = listOf(Book(id = "1"), Book(id = "2")))
        val preference = FakePreference {
            set(activatedBookIdKey, "1")
        }
        val model = createViewModel(authManager, bookRepo, preference)

        model.showFreezeDialog.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `activatedBookId is reset when user is premium`() = runTest {
        val authManager = FakeAuthManager(isPremium = true)
        val preference = FakePreference {
            set(activatedBookIdKey, "1")
        }
        createViewModel(authManager = authManager, preference = preference)

        assertNull(preference.of(activatedBookIdKey).first())
    }

    @Test
    fun `activatedBookId is reset when activated book is deleted`() = runTest {
        val authManager = FakeAuthManager(user = User(premium = false))
        val bookRepo = FakeBookRepo(books = listOf(Book(id = "2")))
        val preference = FakePreference {
            set(activatedBookIdKey, "1")
        }
        val model = createViewModel(authManager, bookRepo, preference)

        model.showFreezeDialog.test {
            assertFalse(awaitItem())
            assertNull(preference.of(activatedBookIdKey).first())
        }
    }

    @Test
    fun `isBookFrozen is true when current book is not the activated one`() = runTest {
        val bookRepo = FakeBookRepo(book = Book(id = "2"))
        val preference = FakePreference {
            set(activatedBookIdKey, "1")
        }
        val model = createViewModel(bookRepo = bookRepo, preference = preference)

        model.isBookFrozen.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `isBookFrozen is false when current book is the activated one`() = runTest {
        val bookRepo = FakeBookRepo(book = Book(id = "1"))
        val preference = FakePreference {
            set(activatedBookIdKey, "1")
        }
        val model = createViewModel(bookRepo = bookRepo, preference = preference)

        model.isBookFrozen.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `activateBook updates preference`() = runTest {
        val preference = FakePreference()
        val model = createViewModel(preference = preference)

        model.activateBook("test-book")
        assertEquals("test-book", preference.of(activatedBookIdKey).first())
    }

    private fun createViewModel(
        authManager: FakeAuthManager = FakeAuthManager(),
        bookRepo: FakeBookRepo = FakeBookRepo(),
        preference: FakePreference = FakePreference(),
    ) = FreezeBookViewModel(
        authManager = authManager,
        bookRepo = bookRepo,
        preference = preference
    )
}

val fakeFreezeBookVm = FreezeBookViewModel(
    authManager = FakeAuthManager(),
    bookRepo = FakeBookRepo(),
    preference = FakePreference()
)