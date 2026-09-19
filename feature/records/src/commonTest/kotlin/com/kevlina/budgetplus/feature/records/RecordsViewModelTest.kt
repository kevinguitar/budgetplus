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
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.ui.bubble.FakeBubbleRepo
import com.kevlina.budgetplus.core.unit.test.BaseTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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

    // displayInPreferredCurrency defaults to true in the fake.
    private fun createCurrencyExchangeRepo() = FakeCurrencyExchangeRepo(
        preferredCurrencyCode = "TWD",
        bookCurrencyRate = 2.0,
    )

    private fun TestScope.createModel(
        currencyExchangeRepo: FakeCurrencyExchangeRepo = createCurrencyExchangeRepo(),
    ): RecordsViewModel {
        val model = RecordsViewModel(
            params = BookDest.Records(
                type = RecordType.Expense,
                category = "Food",
                authorId = null,
            ),
            navController = NavController.preview,
            bookRepo = FakeBookRepo(),
            userRepo = FakeUserRepo(),
            recordRepo = FakeRecordRepo,
            bubbleRepo = FakeBubbleRepo(),
            tracker = FakeTracker(),
            authManager = FakeAuthManager(),
            preference = FakePreference(),
            currencyExchangeRepo = currencyExchangeRepo,
            snackbarSender = FakeSnackbarSender(),
            recordsObserver = FakeRecordsObserver(
                records = sequenceOf(bookCurrencyRecord, preferredCurrencyRecord)
            ),
        )
        backgroundScope.launch(testDispatcher) {
            model.totalPrice.collect()
        }
        return model
    }
}
