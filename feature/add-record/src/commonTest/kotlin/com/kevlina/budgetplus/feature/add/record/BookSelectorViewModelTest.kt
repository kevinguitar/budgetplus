package com.kevlina.budgetplus.feature.add.record

import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.FREE_BOOKS_LIMIT
import com.kevlina.budgetplus.core.data.PREMIUM_BOOKS_LIMIT
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.unit.test.BaseTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BookSelectorViewModelTest : BaseTest() {

    @Test
    fun `createBookBtnState is Enabled when free user has no books`() = runTest {
        val model = createModel(isPremium = false, bookCount = 0)
        assertEquals(CreateBookBtnState.Enabled, model.createBookBtnState.first())
    }

    @Test
    fun `createBookBtnState is NeedPremium when free user reaches free books limit`() = runTest {
        val model = createModel(isPremium = false, bookCount = FREE_BOOKS_LIMIT)
        assertEquals(CreateBookBtnState.NeedPremium, model.createBookBtnState.first())
    }

    @Test
    fun `createBookBtnState is Enabled when premium user is below premium limit`() = runTest {
        val model = createModel(isPremium = true, bookCount = FREE_BOOKS_LIMIT)
        assertEquals(CreateBookBtnState.Enabled, model.createBookBtnState.first())
    }

    @Test
    fun `createBookBtnState is ReachedMax when premium user reaches premium books limit`() = runTest {
        val model = createModel(isPremium = true, bookCount = PREMIUM_BOOKS_LIMIT)
        assertEquals(CreateBookBtnState.ReachedMax, model.createBookBtnState.first())
    }

    @Test
    fun `unlockPremium navigates to UnlockPremium`() = runTest {
        val navController = NavController<BookDest>(startRoot = BookDest.Record)
        val model = createModel(navController = navController)
        model.unlockPremium()

        assertEquals(BookDest.UnlockPremium, navController.backStack.last())
    }

    private fun TestScope.createModel(
        isPremium: Boolean = false,
        bookCount: Int = 0,
        navController: NavController<BookDest> = NavController<BookDest>(startRoot = BookDest.Record),
    ): BookSelectorViewModel {
        val books = (0 until bookCount).map { Book(id = "book_$it") }
        return BookSelectorViewModel(
            bookRepo = FakeBookRepo(books = books),
            snackbarSender = FakeSnackbarSender,
            navController = navController,
            authManager = FakeAuthManager(
                user = User(id = "user", premium = isPremium),
                isPremium = isPremium
            ),
        )
    }
}
