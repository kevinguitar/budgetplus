package com.kevlina.budgetplus.feature.overview

import com.kevlina.budgetplus.core.ads.InterstitialPlacement
import com.kevlina.budgetplus.core.ads.fixtures.FakeInterstitialAdsHandler
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeCurrencyExchangeRepo
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.fixtures.FakeRecordRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeRecordsObserver
import com.kevlina.budgetplus.core.data.fixtures.FakeUserRepo
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.settings.api.ChartModeSettings
import com.kevlina.budgetplus.core.ui.bubble.FakeBubbleRepo
import com.kevlina.budgetplus.core.unit.test.BaseTest
import com.kevlina.budgetplus.feature.overview.utils.CsvExporter
import com.kevlina.budgetplus.feature.overview.utils.CsvSaver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OverviewViewModelTest : BaseTest(useUnconfinedDispatcher = true) {

    // The book currency rate is 2.0, meaning 1 unit of the preferred currency (TWD)
    // equals 2 units of the book currency (KRW).
    private val bookCurrencyExpense = Record(
        id = "1",
        type = RecordType.Expense,
        category = "Food",
        price = 300.0,
    )

    // Recorded as 100 TWD when the rate was 1.1, so a naive doubled conversion of the
    // book price (110 / 2 = 55) differs from the original recorded price.
    private val preferredCurrencyExpense = Record(
        id = "2",
        type = RecordType.Expense,
        category = "Food",
        price = 110.0,
        preferredPrice = 100.0,
        preferredCurrencyCode = "TWD",
    )

    private val bookCurrencyIncome = Record(
        id = "3",
        type = RecordType.Income,
        category = "Salary",
        price = 600.0,
    )

    @Test
    fun `total price sums mixed currency records in the preferred currency`() = runTest {
        val model = createModel()

        // 300 / 2 = 150 for the book currency record, plus the original 100 recorded in TWD.
        assertEquals(
            "250.0 TWD",
            model.state.headerState.totalPrice.first { it.isNotEmpty() }
        )
    }

    @Test
    fun `total price sums record prices in the book currency when the toggle is off`() = runTest {
        val currencyExchangeRepo = createCurrencyExchangeRepo()
        currencyExchangeRepo.toggleDisplayInPreferredCurrency()
        val model = createModel(currencyExchangeRepo = currencyExchangeRepo)

        assertEquals(
            "410.0",
            model.state.headerState.totalPrice.first { it.isNotEmpty() }
        )
    }

    @Test
    fun `balance resolves mixed currency records in the preferred currency`() = runTest {
        val model = createModel()

        // Income: 600 / 2 = 300; expenses: 300 / 2 + 100 = 250.
        assertEquals(
            "50.0 TWD",
            model.state.headerState.balance.first { it.isNotEmpty() }
        )
    }

    @Test
    fun `balance uses the record prices in the book currency when the toggle is off`() = runTest {
        val currencyExchangeRepo = createCurrencyExchangeRepo()
        currencyExchangeRepo.toggleDisplayInPreferredCurrency()
        val model = createModel(currencyExchangeRepo = currencyExchangeRepo)

        // Income: 600; expenses: 300 + 110 = 410.
        assertEquals(
            "190.0",
            model.state.headerState.balance.first { it.isNotEmpty() }
        )
    }

    @Test
    fun `record display prices respect the currency the record was created in`() = runTest {
        val model = createModel()

        val listState = model.state.listState
        // The record was made in the preferred currency, no conversion should happen.
        assertEquals(100.0, listState.getDisplayPrice(preferredCurrencyExpense))
        // The record was made in the book currency, it is converted with the current rate.
        assertEquals(150.0, listState.getDisplayPrice(bookCurrencyExpense))
        assertEquals("100.0 TWD", listState.formatPrice(listState.getDisplayPrice(preferredCurrencyExpense)))
    }

    // displayInPreferredCurrency defaults to true in the fake.
    @Test
    fun `export to csv requests the csv export ad`() = runTest {
        val adsHandler = FakeInterstitialAdsHandler()
        val model = createModel(interstitialAdsHandler = adsHandler)

        model.exportToCsv()

        assertEquals(listOf(InterstitialPlacement.CsvExport), adsHandler.placements)
    }

    private fun createCurrencyExchangeRepo() = FakeCurrencyExchangeRepo(
        preferredCurrencyCode = "TWD",
        bookCurrencyRate = 2.0,
    )

    private fun TestScope.createModel(
        currencyExchangeRepo: FakeCurrencyExchangeRepo = createCurrencyExchangeRepo(),
        interstitialAdsHandler: FakeInterstitialAdsHandler = FakeInterstitialAdsHandler(),
    ): OverviewViewModel {
        val bookId = "book"
        val bookRepo = FakeBookRepo(currentBookId = bookId, book = Book(id = bookId))
        val recordsObserver = FakeRecordsObserver(
            records = sequenceOf(bookCurrencyExpense, preferredCurrencyExpense, bookCurrencyIncome)
        )
        val userRepo = FakeUserRepo()
        val preference = FakePreference()
        val tracker = FakeTracker()
        val authManager = FakeAuthManager()

        return OverviewViewModel(
            recordRepo = FakeRecordRepo,
            recordsObserver = recordsObserver,
            tracker = tracker,
            authManager = authManager,
            userRepo = userRepo,
            bubbleRepo = FakeBubbleRepo(),
            csvExporter = CsvExporter(
                csvSaver = object : CsvSaver {
                    override suspend fun saveToDownload(fileName: String, csvText: String) = Unit
                },
                recordsObserver = recordsObserver,
                userRepo = userRepo,
                bookRepo = bookRepo,
                currencyExchangeRepo = currencyExchangeRepo,
            ),
            snackbarSender = FakeSnackbarSender(),
            interstitialAdsHandler = interstitialAdsHandler,
            navController = NavController.preview,
            bookRepo = bookRepo,
            timeModel = OverviewTimeViewModel(
                navController = NavController.preview,
                recordsObserver = recordsObserver,
                bookRepo = bookRepo,
                authManager = authManager,
                snackbarSender = FakeSnackbarSender(),
                tracker = tracker,
                preference = preference,
            ),
            chartModeSettings = ChartModeSettings(
                appScope = backgroundScope,
                preference = preference,
                tracker = tracker,
            ),
            preference = preference,
            currencyExchangeRepo = currencyExchangeRepo,
            openAppSettingsAction = {},
        )
    }
}
