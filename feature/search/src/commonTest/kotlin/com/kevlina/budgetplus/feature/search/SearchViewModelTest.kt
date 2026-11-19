package com.kevlina.budgetplus.feature.search

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BottomNavTab
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeCurrencyExchangeRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeRecordRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeUserRepo
import com.kevlina.budgetplus.core.data.remote.Author
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.unit.test.BaseTest
import com.kevlina.budgetplus.feature.category.pills.CategoriesViewModel
import com.kevlina.budgetplus.feature.search.fixtures.FakeSearchQueryClient
import com.kevlina.budgetplus.feature.search.ui.SearchCategory
import com.kevlina.budgetplus.feature.search.ui.SearchPeriod
import com.kevlina.budgetplus.feature.search.ui.SearchResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchViewModelTest : BaseTest(
    useUnconfinedDispatcher = true,
    observeComposeSnapshots = true,
) {

    private val me = User(id = "me", name = "Me")
    private val someoneElse = User(id = "someone-else", name = "Someone Else")

    // The book currency rate is 2.0, meaning 1 unit of the preferred currency (TWD)
    // equals 2 units of the book currency.
    private val bookCurrencyBreakfast = Record(
        id = "book-currency-breakfast",
        type = RecordType.Expense,
        category = "Food",
        name = "Breakfast",
        price = 300.0,
        author = Author(id = me.id, name = me.name),
    )

    // Recorded as 100 TWD, so its original price is summed instead of converting 110 back.
    private val preferredCurrencyBreakfast = Record(
        id = "preferred-currency-breakfast",
        type = RecordType.Expense,
        category = "Food",
        name = "Breakfast",
        price = 110.0,
        preferredPrice = 100.0,
        preferredCurrencyCode = "TWD",
        author = Author(id = someoneElse.id, name = someoneElse.name),
    )

    private val breakfastTaxi = Record(
        id = "breakfast-taxi",
        type = RecordType.Expense,
        category = "Transport",
        name = "Breakfast taxi",
        price = 40.0,
        author = Author(id = me.id, name = me.name),
    )

    private val breakfastIncome = Record(
        id = "breakfast-income",
        type = RecordType.Income,
        category = "Salary",
        name = "Breakfast allowance",
        price = 1000.0,
        author = Author(id = me.id, name = me.name),
    )

    private val lunch = Record(
        id = "lunch",
        type = RecordType.Expense,
        category = "Food",
        name = "Lunch",
        price = 500.0,
        author = Author(id = me.id, name = me.name),
    )

    private val allRecords = listOf(
        bookCurrencyBreakfast,
        preferredCurrencyBreakfast,
        breakfastTaxi,
        breakfastIncome,
        lunch,
    )

    @Test
    fun `totalPrice is null before searching`() = runTest {
        val model = createModel()

        model.totalPrice.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `totalPrice sums the matching records in the preferred currency`() = runTest {
        val model = createModel()

        model.totalPrice.test {
            assertNull(awaitItem())
            search("breakfast")
            // 300 / 2 = 150 + the recorded 100 TWD + 40 / 2 = 20. The income and lunch are excluded.
            assertEquals("270.0 TWD", awaitItem())
        }
    }

    @Test
    fun `totalPrice follows the preferred currency toggle`() = runTest {
        val model = createModel()

        model.totalPrice.test {
            assertNull(awaitItem())
            search("breakfast")
            assertEquals("270.0 TWD", awaitItem())

            currencyExchangeRepo.toggleDisplayInPreferredCurrency()
            // 300 + 110 + 40 in the book currency.
            assertEquals("450.0", awaitItem())
        }
    }

    @Test
    fun `totalPrice becomes null when no record matches`() = runTest {
        val model = createModel()

        model.totalPrice.test {
            assertNull(awaitItem())
            search("breakfast")
            assertEquals("270.0 TWD", awaitItem())

            searchRepo.query.setTextAndPlaceCursorAtEnd("dinner")
            assertNull(awaitItem())
        }
    }

    @Test
    fun `totalPrice only sums the records of the selected type`() = runTest {
        val model = createModel()

        model.totalPrice.test {
            assertNull(awaitItem())
            search("breakfast")
            assertEquals("270.0 TWD", awaitItem())

            model.state.filter.selectType(RecordType.Income)
            // 1000 / 2 for the breakfast allowance.
            assertEquals("500.0 TWD", awaitItem())
        }
    }

    @Test
    fun `search matches the query against record categories`() = runTest {
        val model = createModel()

        model.state.result.result.test {
            assertEquals(SearchResult.Empty, awaitItem())
            search("Food")
            assertEquals(SearchResult.Loading, awaitItem())
            assertEquals(
                listOf(bookCurrencyBreakfast, preferredCurrencyBreakfast, lunch),
                awaitSuccessRecords()
            )
        }
    }

    @Test
    fun `search filters by the selected category`() = runTest {
        val model = createModel()

        model.state.result.result.test {
            assertEquals(SearchResult.Empty, awaitItem())
            search("breakfast")
            assertEquals(SearchResult.Loading, awaitItem())
            assertEquals(
                listOf(bookCurrencyBreakfast, preferredCurrencyBreakfast, breakfastTaxi),
                awaitSuccessRecords()
            )

            model.state.filter.selectCategory(SearchCategory.Selected("Transport"))
            assertEquals(listOf(breakfastTaxi), awaitSuccessRecords())
        }
    }

    @Test
    fun `search filters by the selected author`() = runTest {
        val model = createModel()

        model.state.result.result.test {
            assertEquals(SearchResult.Empty, awaitItem())
            search("breakfast")
            assertEquals(SearchResult.Loading, awaitItem())
            assertEquals(
                listOf(bookCurrencyBreakfast, preferredCurrencyBreakfast, breakfastTaxi),
                awaitSuccessRecords()
            )

            model.state.filter.selectAuthor(someoneElse)
            assertEquals(listOf(preferredCurrencyBreakfast), awaitSuccessRecords())
        }
    }

    @Test
    fun `selecting another type resets the category`() = runTest {
        val model = createModel()
        model.state.filter.selectCategory(SearchCategory.Selected("Food"))

        model.state.filter.selectType(RecordType.Income)

        assertEquals(RecordType.Income, model.state.filter.type.value)
        assertEquals(SearchCategory.None, model.state.filter.category.value)
    }

    @Test
    fun `selecting the same type keeps the category`() = runTest {
        val model = createModel()
        model.state.filter.selectCategory(SearchCategory.Selected("Food"))

        model.state.filter.selectType(RecordType.Expense)

        assertEquals(SearchCategory.Selected("Food"), model.state.filter.category.value)
    }

    @Test
    fun `tapping a category pill selects it and tapping it again clears it`() = runTest {
        val model = createModel()
        val grid = model.state.filter.categoryGrid

        grid.onCategorySelected("Food")
        assertEquals(SearchCategory.Selected("Food"), model.state.filter.category.value)

        grid.onCategorySelected("Food")
        assertEquals(SearchCategory.None, model.state.filter.category.value)
    }

    @Test
    fun `free users are sent to the paywall for premium periods`() = runTest {
        val model = createModel(authManager = FakeAuthManager(user = me, isPremium = false))

        model.state.filter.selectPeriod(SearchPeriod.PastYear)

        assertEquals(SearchPeriod.PastMonth, searchRepo.period.value)
        assertEquals(BookDest.UnlockPremium, navController.backStack.last())
        assertEquals("search_period_unlock_premium", tracker.lastEventName)
    }

    @Test
    fun `free users can select the past month`() = runTest {
        val model = createModel(authManager = FakeAuthManager(user = me, isPremium = false))
        searchRepo.period.value = SearchPeriod.Custom.Unselected

        model.state.filter.selectPeriod(SearchPeriod.PastMonth)

        assertEquals(SearchPeriod.PastMonth, searchRepo.period.value)
        assertEquals(BottomNavTab.Add.root, navController.backStack.last())
    }

    @Test
    fun `premium users can select premium periods`() = runTest {
        val model = createModel(authManager = FakeAuthManager(user = me, isPremium = true))

        model.state.filter.selectPeriod(SearchPeriod.PastYear)

        assertEquals(SearchPeriod.PastYear, searchRepo.period.value)
        assertEquals(BottomNavTab.Add.root, navController.backStack.last())
    }

    @Test
    fun `allAuthors resolves the book authors`() = runTest {
        val model = createModel(
            bookRepo = FakeBookRepo(
                book = Book(authors = listOf(me.id, someoneElse.id, "unknown")),
                currentBookId = BOOK_ID,
            ),
        )

        model.state.filter.allAuthors.test {
            assertEquals(emptyList(), awaitItem())
            // Authors that can't be resolved are dropped.
            assertEquals(listOf(me, someoneElse), awaitItem())
        }
    }

    @Test
    fun `members can only edit their own records when the book disallows editing`() = runTest {
        val model = createModel(bookRepo = FakeBookRepo(currentBookId = BOOK_ID, canEdit = false))

        model.state.result.result.test {
            assertEquals(SearchResult.Empty, awaitItem())
            search("breakfast")
            assertEquals(SearchResult.Loading, awaitItem())

            val states = (awaitItem() as SearchResult.Success).records
            assertEquals(
                mapOf(
                    bookCurrencyBreakfast.id to true,
                    preferredCurrencyBreakfast.id to false,
                    breakfastTaxi.id to true,
                ),
                states.associate { it.item.id to it.canEdit }
            )
        }
    }

    @Test
    fun `record cards format prices and mark the last item`() = runTest {
        val model = createModel()

        model.state.result.result.test {
            assertEquals(SearchResult.Empty, awaitItem())
            search("breakfast")
            assertEquals(SearchResult.Loading, awaitItem())

            val states = (awaitItem() as SearchResult.Success).records
            assertEquals(
                listOf("150.0 TWD", "100.0 TWD", "20.0 TWD"),
                states.map { it.formattedPrice }
            )
            assertEquals(listOf(false, false, true), states.map { it.isLast })
        }
    }

    @Test
    fun `record card actions send the edit and delete events`() = runTest {
        val model = createModel()

        model.state.result.result.test {
            assertEquals(SearchResult.Empty, awaitItem())
            search("breakfast")
            assertEquals(SearchResult.Loading, awaitItem())

            val state = (awaitItem() as SearchResult.Success).records.first()
            state.onEdit()
            state.onDelete()

            assertEquals(bookCurrencyBreakfast, model.state.result.editRecordEvent.value.consume())
            assertEquals(bookCurrencyBreakfast, model.state.result.deleteRecordEvent.value.consume())
        }
    }

    /**
     *  Types the [query] and delivers [records] from the DB. The DB query is only executed
     *  while the search result is collected, so call it inside a collection.
     */
    private suspend fun search(query: String, records: List<Record> = allRecords) {
        searchRepo.query.setTextAndPlaceCursorAtEnd(query)
        searchQueryClient.recordsFlow.subscriptionCount.first { it > 0 }
        searchQueryClient.recordsFlow.emit(records)
    }

    private suspend fun ReceiveTurbine<SearchResult>.awaitSuccessRecords(): List<Record> {
        return (awaitItem() as SearchResult.Success).records.map { it.item }
    }

    private val searchQueryClient = FakeSearchQueryClient()
    private val tracker = FakeTracker()
    private val navController = NavController<BookDest>(startRoot = BottomNavTab.Add.root)
    private val currencyExchangeRepo = FakeCurrencyExchangeRepo(
        preferredCurrencyCode = "TWD",
        bookCurrencyRate = 2.0,
    )

    private lateinit var searchRepo: SearchRepo

    private fun createModel(
        bookRepo: FakeBookRepo = FakeBookRepo(currentBookId = BOOK_ID),
        authManager: FakeAuthManager = FakeAuthManager(user = me),
    ): SearchViewModel {
        searchRepo = SearchRepo(
            searchQueryClient = searchQueryClient,
            bookRepo = bookRepo,
            snackbarSender = FakeSnackbarSender(),
            tracker = tracker,
        )
        return SearchViewModel(
            params = BookDest.Search(type = RecordType.Expense),
            searchRepo = searchRepo,
            authManager = authManager,
            bookRepo = bookRepo,
            currencyExchangeRepo = currencyExchangeRepo,
            recordRepo = FakeRecordRepo,
            userRepo = FakeUserRepo(users = listOf(me, someoneElse).associateBy { it.id }),
            tracker = tracker,
            navController = navController,
            categoriesVm = CategoriesViewModel(bookRepo = bookRepo),
        )
    }

    private companion object {
        const val BOOK_ID = "book_id"
    }
}
