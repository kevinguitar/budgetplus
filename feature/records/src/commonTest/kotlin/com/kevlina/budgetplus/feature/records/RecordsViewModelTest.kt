package com.kevlina.budgetplus.feature.records

import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeCurrencyExchangeRepo
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.fixtures.FakeRecordRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeRecordsObserver
import com.kevlina.budgetplus.core.data.fixtures.FakeUserRepo
import com.kevlina.budgetplus.core.data.remote.Author
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.data.remote.User
import com.kevlina.budgetplus.core.ui.bubble.FakeBubbleRepo
import com.kevlina.budgetplus.core.unit.test.BaseTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordsViewModelTest : BaseTest(useUnconfinedDispatcher = true) {

    // The book currency rate is 2.0, meaning 1 unit of the preferred currency (TWD)
    // equals 2 units of the book currency (KRW).
    private val bookCurrencyRecord = Record(
        id = "1",
        type = RecordType.Expense,
        category = "Food",
        price = 300.0,
    )

    // Recorded as 100 TWD when the rate was 1.1, so a naive doubled conversion of the
    // book price (110 / 2 = 55) differs from the original recorded price.
    private val preferredCurrencyRecord = Record(
        id = "2",
        type = RecordType.Expense,
        category = "Food",
        price = 110.0,
        preferredPrice = 100.0,
        preferredCurrencyCode = "TWD",
    )

    @Test
    fun `total price sums mixed currency records in the preferred currency`() = runTest {
        val currencyExchangeRepo = createCurrencyExchangeRepo()
        val model = createModel(currencyExchangeRepo = currencyExchangeRepo)

        // 300 / 2 = 150 for the book currency record, plus the original 100 recorded in TWD.
        assertEquals("250.0 TWD", model.totalPrice.value)
    }

    @Test
    fun `total price sums record prices in the book currency when the toggle is off`() = runTest {
        val currencyExchangeRepo = createCurrencyExchangeRepo()
        currencyExchangeRepo.toggleDisplayInPreferredCurrency()
        val model = createModel(currencyExchangeRepo = currencyExchangeRepo)

        assertEquals("410.0", model.totalPrice.value)
    }

    @Test
    fun `formatRecordPrice uses the recorded preferred price to avoid a doubled conversion`() = runTest {
        val currencyExchangeRepo = createCurrencyExchangeRepo()
        val model = createModel(currencyExchangeRepo = currencyExchangeRepo)

        assertEquals("100.0 TWD", model.formatRecordPrice(preferredCurrencyRecord))
    }

    @Test
    fun `formatRecordPrice converts records that were made in the book currency`() = runTest {
        val currencyExchangeRepo = createCurrencyExchangeRepo()
        val model = createModel(currencyExchangeRepo = currencyExchangeRepo)

        assertEquals("150.0 TWD", model.formatRecordPrice(bookCurrencyRecord))
    }

    @Test
    fun `formatRecordPrice shows the original prices when the toggle is off`() = runTest {
        val currencyExchangeRepo = createCurrencyExchangeRepo()
        currencyExchangeRepo.toggleDisplayInPreferredCurrency()
        val model = createModel(currencyExchangeRepo = currencyExchangeRepo)

        assertEquals("300.0", model.formatRecordPrice(bookCurrencyRecord))
        assertEquals("110.0", model.formatRecordPrice(preferredCurrencyRecord))
    }

    @Test
    fun `canDeleteSelected is false when nothing is selected`() = runTest {
        val model = createModel()
        assertFalse(model.canDeleteSelected.value)
    }

    @Test
    fun `canDeleteSelected is true when all selected records are editable`() = runTest {
        // canEdit=true on the book -> every record is editable regardless of author.
        val model = createModel()

        model.startSelection(bookCurrencyRecord)
        model.toggleSelection(preferredCurrencyRecord)

        assertTrue(model.canDeleteSelected.value)
    }

    @Test
    fun `canDeleteSelected is false when a selected record cannot be edited`() = runTest {
        // canEdit=false and the record's author differs from the current user -> not editable.
        val othersRecord = bookCurrencyRecord.copy(
            id = "other",
            author = Author(id = "someone-else", name = "Someone Else")
        )
        val model = createModel(
            bookRepo = FakeBookRepo(canEdit = false),
            authManager = FakeAuthManager(user = User(id = "me", name = "Me")),
            records = sequenceOf(othersRecord),
        )

        model.startSelection(othersRecord)

        assertFalse(model.canDeleteSelected.value)
    }

    @Test
    fun `canDeleteSelected is false when the selection mixes editable and non-editable records`() = runTest {
        val myRecord = bookCurrencyRecord.copy(
            id = "mine",
            author = Author(id = "me", name = "Me")
        )
        val othersRecord = bookCurrencyRecord.copy(
            id = "other",
            author = Author(id = "someone-else", name = "Someone Else")
        )
        val model = createModel(
            bookRepo = FakeBookRepo(canEdit = false),
            authManager = FakeAuthManager(user = User(id = "me", name = "Me")),
            records = sequenceOf(myRecord, othersRecord),
        )

        model.startSelection(myRecord)
        model.toggleSelection(othersRecord)

        assertFalse(model.canDeleteSelected.value)
    }

    // displayInPreferredCurrency defaults to true in the fake.
    private fun createCurrencyExchangeRepo() = FakeCurrencyExchangeRepo(
        preferredCurrencyCode = "TWD",
        bookCurrencyRate = 2.0,
    )

    private fun TestScope.createModel(
        currencyExchangeRepo: FakeCurrencyExchangeRepo = createCurrencyExchangeRepo(),
        bookRepo: FakeBookRepo = FakeBookRepo(),
        authManager: FakeAuthManager = FakeAuthManager(),
        records: Sequence<Record> = sequenceOf(bookCurrencyRecord, preferredCurrencyRecord),
    ): RecordsViewModel {
        val model = RecordsViewModel(
            params = BookDest.Records(
                type = RecordType.Expense,
                category = "Food",
                authorId = null,
            ),
            navController = NavController.preview,
            bookRepo = bookRepo,
            userRepo = FakeUserRepo(),
            recordRepo = FakeRecordRepo,
            bubbleRepo = FakeBubbleRepo(),
            tracker = FakeTracker(),
            authManager = authManager,
            preference = FakePreference(),
            currencyExchangeRepo = currencyExchangeRepo,
            snackbarSender = FakeSnackbarSender(),
            recordsObserver = FakeRecordsObserver(
                records = records
            ),
        )
        backgroundScope.launch(testDispatcher) {
            model.totalPrice.collect()
        }
        backgroundScope.launch(testDispatcher) {
            model.canDeleteSelected.collect()
        }
        return model
    }
}
