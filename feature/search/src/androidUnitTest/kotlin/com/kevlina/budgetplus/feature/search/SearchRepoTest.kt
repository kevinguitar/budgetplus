package com.kevlina.budgetplus.feature.search

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.unit.test.BaseTest
import com.kevlina.budgetplus.feature.search.SearchRepo.DbResult
import com.kevlina.budgetplus.feature.search.ui.SearchCategory
import dev.gitlive.firebase.firestore.CollectionReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchRepoTest : BaseTest(observeComposeSnapshots = true) {

    @Test
    fun `do not execute DB query on repo init`() = runTest {
        repo.dbResult.test {
            expectNoEvents()
        }
    }

    @Test
    fun `do not execute DB query when the query text is blank`() = runTest {
        repo.dbResult.test {
            repo.query.setTextAndPlaceCursorAtEnd("    ")
            expectNoEvents()
        }
    }

    @Test
    fun `execute DB query when the query text is presented`() = runTest {
        repo.dbResult.test {
            repo.query.setTextAndPlaceCursorAtEnd("search")
            assertEquals(DbResult.Loading, awaitItem())
        }
    }

    @Test
    fun `execute DB query when the category is selected`() = runTest {
        repo.dbResult.test {
            repo.category.value = SearchCategory.Selected("food")
            assertEquals(DbResult.Loading, awaitItem())
        }
    }

    @Test
    fun `do not execute DB query again upon text changes`() = runTest {
        repo.dbResult.test {
            repo.query.setTextAndPlaceCursorAtEnd("search")
            assertEquals(DbResult.Loading, awaitItem())

            repo.query.setTextAndPlaceCursorAtEnd("search 1")
            repo.query.setTextAndPlaceCursorAtEnd("search 2")
            repo.query.setTextAndPlaceCursorAtEnd("search 3")
            expectNoEvents()
            verify(exactly = 1) { booksDb.document(any()) }
        }
    }

    @Test
    fun `do not execute DB query again upon category changes`() = runTest {
        repo.dbResult.test {
            repo.category.value = SearchCategory.Selected("food")
            assertEquals(DbResult.Loading, awaitItem())

            repo.category.value = SearchCategory.Selected("daily")
            repo.category.value = SearchCategory.Selected("transport")
            repo.category.value = SearchCategory.Selected("loan")
            expectNoEvents()
            verify(exactly = 1) { booksDb.document(any()) }
        }
    }

    private val booksDb = mockk<CollectionReference> {
        every { document(any()) } returns mockk(relaxed = true)
    }

    private val repo = SearchRepo(
        booksDb = lazy { booksDb },
        bookRepo = FakeBookRepo(currentBookId = "book_id"),
        snackbarSender = FakeSnackbarSender,
        tracker = FakeTracker()
    )
}