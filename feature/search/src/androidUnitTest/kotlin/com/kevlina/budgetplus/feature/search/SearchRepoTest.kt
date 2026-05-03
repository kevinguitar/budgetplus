package com.kevlina.budgetplus.feature.search

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.unit.test.BaseTest
import com.kevlina.budgetplus.feature.search.SearchRepo.DbResult
import com.kevlina.budgetplus.feature.search.fixtures.FakeSearchQueryClient
import com.kevlina.budgetplus.feature.search.ui.SearchCategory
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
            assertEquals(1, searchQueryClient.queryCalls.size)
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
            assertEquals(1, searchQueryClient.queryCalls.size)
        }
    }

    private val searchQueryClient = FakeSearchQueryClient()

    private val repo = SearchRepo(
        searchQueryClient = searchQueryClient,
        bookRepo = FakeBookRepo(currentBookId = "book_id"),
        snackbarSender = FakeSnackbarSender,
        tracker = FakeTracker()
    )
}