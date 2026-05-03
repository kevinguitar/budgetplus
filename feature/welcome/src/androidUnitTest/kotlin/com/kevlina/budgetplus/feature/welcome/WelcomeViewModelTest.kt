package com.kevlina.budgetplus.feature.welcome

import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.unit.test.MainDispatcherRule
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertFalse

class WelcomeViewModelTest {

    @get:Rule
    val rule = MainDispatcherRule()

    @Test
    fun `isCreatingBook is initially false`() = runTest {
        val model = createModel()
        assertFalse(model.isCreatingBook.value)
    }

    @Test
    fun `navigates to Record when books are available`() = runTest {
        val navController = NavController(startRoot = BookDest.Welcome)
        val bookRepo = FakeBookRepo(books = emptyList())

        createModel(bookRepo = bookRepo, navController = navController)

        // Simulate books becoming available
        bookRepo.booksState.value = listOf(Book(id = "book_1"))

        // After books become non-empty, the ViewModel navigates to Record
        val lastDest = navController.backStack.last()
        assert(lastDest is BookDest.Record) {
            "Expected navigation to BookDest.Record but was $lastDest"
        }
    }

    @Test
    fun `does not navigate when books list is null`() = runTest {
        val navController = NavController(startRoot = BookDest.Welcome)
        val bookRepo = FakeBookRepo(books = null)

        createModel(bookRepo = bookRepo, navController = navController)

        // Books remain null, should stay on welcome
        val lastDest = navController.backStack.last()
        assert(lastDest is BookDest.Welcome) {
            "Expected to remain on BookDest.Welcome but was $lastDest"
        }
    }

    private fun TestScope.createModel(
        bookRepo: FakeBookRepo = FakeBookRepo(books = emptyList()),
        navController: NavController<BookDest> = NavController(startRoot = BookDest.Welcome),
    ): WelcomeViewModel {
        return WelcomeViewModel(
            snackbarSender = FakeSnackbarSender,
            bookRepo = bookRepo,
            authManager = FakeAuthManager(),
            navController = navController,
        )
    }
}
