package com.kevlina.budgetplus.feature.add.record

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.datastore.preferences.core.intPreferencesKey
import app.cash.turbine.test
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.record_currency_rate_unavailable
import budgetplus.core.common.generated.resources.record_empty_category
import budgetplus.core.common.generated.resources.record_empty_price
import com.kevlina.budgetplus.core.ads.fixtures.FakeInterstitialAdsHandler
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.ExpressionEvaluator
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.fixtures.FakeShareHelper
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.data.CurrencyExchangeRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeCurrencyExchangeRepo
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.fixtures.FakeRecordRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeVibratorManager
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.ui.bubble.FakeBubbleRepo
import com.kevlina.budgetplus.core.unit.test.BaseTest
import com.kevlina.budgetplus.feature.category.pills.CategoriesViewModel
import com.kevlina.budgetplus.feature.freeze.createFreezeBookVm
import com.kevlina.budgetplus.feature.speak.record.RecordActor
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecord
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecordViewModel
import com.kevlina.budgetplus.inapp.review.fixtures.FakeInAppReviewManager
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordViewModelTest : BaseTest(useUnconfinedDispatcher = true) {

    @Test
    fun `show message when category is empty`() = runTest {
        val model = createModel()
        categoriesVm.setCategory(null)
        model.calculatorVm.input("1")
        model.calculatorVm.evaluate()

        assertEquals(Res.string.record_empty_category, FakeSnackbarSender.lastSentMessageRes)
    }

    @Test
    fun `show message when price is empty`() = runTest {
        val model = createModel()
        model.calculatorVm.evaluate()

        assertEquals(Res.string.record_empty_price, FakeSnackbarSender.lastSentMessageRes)
    }

    @Test
    fun `record should be created with correct info in RecordRepo`() = runTest {
        val date = LocalDate.now().minus(1, DateTimeUnit.DAY)

        val model = createModel().apply {
            setDate(date)
            note.setTextAndPlaceCursorAtEnd("Test note")
            setType(RecordType.Income)
        }

        model.calculatorVm.input("123")
        model.calculatorVm.evaluate()

        assertEquals(
            Record(
                type = RecordType.Income,
                category = "Test category",
                name = "Test note",
                date = date.toEpochDays(),
                price = 123.0,
            ),
            // Do not verify timestamp as it depends on test execution time
            FakeRecordRepo.lastCreatedRecord?.copy(timestamp = null)
        )
    }

    @Test
    fun `record without note should fallback to category`() = runTest {
        val model = createModel()

        model.calculatorVm.input("1.23")
        model.calculatorVm.evaluate()

        assertEquals(
            Record(
                type = RecordType.Expense,
                category = "Test category",
                name = "Test category",
                date = LocalDate.now().toEpochDays(),
                price = 1.23,
            ),
            // Do not verify timestamp as it depends on test execution time
            FakeRecordRepo.lastCreatedRecord?.copy(timestamp = null)
        )
    }

    @Test
    fun `reset screen after the record is recorded`() = runTest {
        val model = createModel()
        model.note.setTextAndPlaceCursorAtEnd("Test note")

        model.calculatorVm.input("1")
        model.calculatorVm.evaluate()

        assertNull(categoriesVm.category.value)
        assertTrue(model.note.text.toString().isEmpty())
        assertEquals("0", model.calculatorVm.priceText.text)
    }

    @Test
    fun `show fullscreen ad on every 7th record`() = runTest {
        val model = createModel(recordCount = 6)
        model.calculatorVm.input("1")
        model.calculatorVm.evaluate()

        assertEquals(1, interstitialAdsHandler.count)
    }

    @Test
    fun `request notification permission after the 2nd record`() = runTest {
        val model = createModel(recordCount = 1)
        model.calculatorVm.input("1")
        model.calculatorVm.evaluate()

        model.requestPermissionEvent.awaitUnconsumedEvent()
    }

    @Test
    fun `request in app review after 4th record`() = runTest {
        val model = createModel(recordCount = 3)
        model.calculatorVm.input("1")
        model.calculatorVm.evaluate()

        model.requestReviewEvent.awaitUnconsumedEvent()
    }

    @Test
    fun `editCurrency should navigate to BookCurrency when user can edit book`() = runTest {
        val model = createModel(canEditBook = true)
        model.editCurrency()

        assertEquals(
            BookDest.CurrencyPicker(purpose = BookDest.CurrencyPicker.Purpose.Book),
            model.navController.backStack.last()
        )
    }

    @Test
    fun `editCurrency should not navigate when user cannot edit book`() = runTest {
        val model = createModel(canEditBook = false)
        val initialDest = model.navController.backStack.last()
        model.editCurrency()

        assertEquals(initialDest, model.navController.backStack.last())
    }

    @Test
    fun `editPreferredCurrency should navigate to PreferredCurrency when user is premium`() = runTest {
        val model = createModel(isPremium = true)
        model.editPreferredCurrency()

        assertEquals(
            BookDest.CurrencyPicker(purpose = BookDest.CurrencyPicker.Purpose.Preferred),
            model.navController.backStack.last()
        )
    }

    @Test
    fun `editPreferredCurrency should navigate to UnlockPremium when user is not premium`() = runTest {
        val model = createModel(isPremium = false)
        model.editPreferredCurrency()

        assertEquals(BookDest.UnlockPremium, model.navController.backStack.last())
    }

    @Test
    fun `convertedPrice should emit formatted price from currencyExchangeRepo`() = runTest {
        val currencyExchangeRepo = FakeCurrencyExchangeRepo(preferredCurrencyCode = "EUR")
        val model = createModel(currencyExchangeRepo = currencyExchangeRepo)

        model.calculatorVm.input("100")
        assertEquals("100.0 EUR", model.convertedPrice.first { it != null })
    }

    @Test
    fun `convertedPrice should show book currency amount when preferred currency is selected`() = runTest {
        val currencyExchangeRepo = FakeCurrencyExchangeRepo(
            preferredCurrencyCode = "EUR",
            bookCurrencyRate = 0.5,
        )
        val model = createModel(isPremium = true, currencyExchangeRepo = currencyExchangeRepo)
        model.onPreferredCurrencyClick()

        model.calculatorVm.input("100")
        assertEquals("50.0", model.convertedPrice.first { it != null })
    }

    @Test
    fun `convertedPrice should emit null when preferred is selected but rate is unavailable`() = runTest {
        val currencyExchangeRepo = FakeCurrencyExchangeRepo(
            preferredCurrencyCode = "EUR",
            bookCurrencyRate = null,
        )
        val model = createModel(isPremium = true, currencyExchangeRepo = currencyExchangeRepo)
        model.onPreferredCurrencyClick()

        model.convertedPrice.test {
            assertNull(awaitItem()) // Initial value
            model.calculatorVm.input("100")
            // The rate is unavailable so the converted price stays null.
            expectNoEvents()
        }
    }

    @Test
    fun `onBookCurrencyClick should select the book currency when preferred is selected`() = runTest {
        val model = createModel(isPremium = true)
        model.selectedCurrency.test {
            skipItems(1) // Skip initial value
            model.onPreferredCurrencyClick()
            assertEquals(SelectedCurrency.Preferred, awaitItem())

            model.onBookCurrencyClick()
            assertEquals(SelectedCurrency.Book, awaitItem())
        }
    }

    @Test
    fun `onBookCurrencyClick should edit the book currency when already selected`() = runTest {
        val model = createModel(canEditBook = true)
        // Book currency is selected by default.
        model.onBookCurrencyClick()

        assertEquals(
            BookDest.CurrencyPicker(purpose = BookDest.CurrencyPicker.Purpose.Book),
            model.navController.backStack.last()
        )
    }

    @Test
    fun `onPreferredCurrencyClick should select the preferred currency when premium`() = runTest {
        val model = createModel(isPremium = true)
        model.selectedCurrency.test {
            assertEquals(SelectedCurrency.Book, awaitItem()) // Initial value
            model.onPreferredCurrencyClick()
            assertEquals(SelectedCurrency.Preferred, awaitItem())
        }
    }

    @Test
    fun `onPreferredCurrencyClick should navigate to UnlockPremium when not premium`() = runTest {
        val model = createModel(isPremium = false)
        model.onPreferredCurrencyClick()

        assertEquals(SelectedCurrency.Book, model.selectedCurrency.value)
        assertEquals(BookDest.UnlockPremium, model.navController.backStack.last())
    }

    @Test
    fun `onPreferredCurrencyClick should edit the preferred currency when already selected`() = runTest {
        val model = createModel(isPremium = true)
        model.selectedCurrency.test {
            skipItems(1) // Skip initial value
            model.onPreferredCurrencyClick()
            assertEquals(SelectedCurrency.Preferred, awaitItem())
            // Now the preferred currency is selected, clicking again edits it.
            model.onPreferredCurrencyClick()
        }

        assertEquals(
            BookDest.CurrencyPicker(purpose = BookDest.CurrencyPicker.Purpose.Preferred),
            model.navController.backStack.last()
        )
    }

    @Test
    fun `editPreferredCurrency should log tracker event`() = runTest {
        val model = createModel(isPremium = true)
        model.editPreferredCurrency()

        assertEquals("currency_exchange_edit_preferred", tracker.lastEventName)
    }

    @Test
    fun `record should be created with preferred currency info when preferred is selected`() = runTest {
        val currencyExchangeRepo = FakeCurrencyExchangeRepo(
            preferredCurrencyCode = "EUR",
            bookCurrencyRate = 0.5,
        )
        val model = createModel(isPremium = true, currencyExchangeRepo = currencyExchangeRepo)
        model.selectedCurrency.test {
            skipItems(1) // Skip initial value
            model.onPreferredCurrencyClick()
            assertEquals(SelectedCurrency.Preferred, awaitItem())

            model.calculatorVm.input("100")
            model.calculatorVm.evaluate()
        }

        assertEquals(
            Record(
                type = RecordType.Expense,
                category = "Test category",
                name = "Test category",
                date = LocalDate.now().toEpochDays(),
                price = 50.0,
                preferredPrice = 100.0,
                preferredCurrencyCode = "EUR",
            ),
            // Do not verify timestamp as it depends on test execution time
            FakeRecordRepo.lastCreatedRecord?.copy(timestamp = null)
        )
    }

    @Test
    fun `record should show message when preferred currency rate is unavailable`() = runTest {
        FakeRecordRepo.lastCreatedRecord = null
        val currencyExchangeRepo = FakeCurrencyExchangeRepo(
            preferredCurrencyCode = "EUR",
            bookCurrencyRate = null,
        )
        val model = createModel(isPremium = true, currencyExchangeRepo = currencyExchangeRepo)
        model.selectedCurrency.test {
            skipItems(1) // Skip initial value
            model.onPreferredCurrencyClick()
            assertEquals(SelectedCurrency.Preferred, awaitItem())

            model.calculatorVm.input("100")
            model.calculatorVm.evaluate()
        }

        assertEquals(
            Res.string.record_currency_rate_unavailable,
            FakeSnackbarSender.lastSentMessageRes
        )
        assertNull(FakeRecordRepo.lastCreatedRecord)
    }

    @Test
    fun `selected currency should be remembered after recording`() = runTest {
        val model = createModel(isPremium = true)
        model.selectedCurrency.test {
            skipItems(1) // Skip initial value
            model.onPreferredCurrencyClick()
            assertEquals(SelectedCurrency.Preferred, awaitItem())

            model.calculatorVm.input("1")
            model.calculatorVm.evaluate()

            // The selection is unchanged after recording.
            expectNoEvents()
        }
    }


    @Test
    fun `selected currency should be remembered per book across recreation`() = runTest {
        val bookRepo = FakeBookRepo(
            book = Book(id = "bookA"),
            currentBookId = "bookA",
        )
        val preference = FakePreference {
            set(intPreferencesKey("recordCount"), 0)
        }

        val model = createModel(isPremium = true, bookRepo = bookRepo, preference = preference)
        model.selectedCurrency.test {
            skipItems(1) // Skip initial value
            model.onPreferredCurrencyClick()
            assertEquals(SelectedCurrency.Preferred, awaitItem())
        }

        // A freshly created view model (e.g. after reopening the app) restores the selection.
        val recreatedModel = createModel(isPremium = true, bookRepo = bookRepo, preference = preference)
        assertEquals(
            SelectedCurrency.Preferred,
            recreatedModel.selectedCurrency.first { it == SelectedCurrency.Preferred }
        )
    }

    private fun TestScope.createCalculatorVm() = CalculatorViewModel(
        vibrator = FakeVibratorManager(),
        snackbarSender = FakeSnackbarSender,
        speakToRecordVm = fakeSpeakToRecordVm,
        freezeBookVm = createFreezeBookVm(),
        expressionEvaluator = ExpressionEvaluator(),
        appScope = backgroundScope
    )

    private val bookRepo = FakeBookRepo()
    private val categoriesVm = CategoriesViewModel(bookRepo = bookRepo).apply {
        setCategory("Test category")
    }

    private val interstitialAdsHandler = FakeInterstitialAdsHandler()
    private val tracker = FakeTracker()

    private fun TestScope.createModel(
        recordCount: Int = 0,
        canEditBook: Boolean = true,
        isPremium: Boolean = false,
        currencyExchangeRepo: CurrencyExchangeRepo = FakeCurrencyExchangeRepo(),
        calculatorVm: CalculatorViewModel = createCalculatorVm(),
        bookRepo: FakeBookRepo = FakeBookRepo(
            book = Book(id = "book"),
            currentBookId = "book",
            canEdit = canEditBook,
        ),
        preference: FakePreference = FakePreference {
            set(intPreferencesKey("recordCount"), recordCount)
        },
    ) = RecordViewModel(
        calculatorVm = calculatorVm,
        categoriesVm = categoriesVm,
        freezeBookVm = createFreezeBookVm(),
        bookRepo = bookRepo,
        navController = NavController(startRoot = BookDest.Record),
        recordRepo = FakeRecordRepo,
        bubbleRepo = FakeBubbleRepo(),
        authManager = FakeAuthManager(isPremium = isPremium),
        interstitialAdsHandler = interstitialAdsHandler,
        inAppReviewManager = FakeInAppReviewManager(),
        currencyExchangeRepo = currencyExchangeRepo,
        snackbarSender = FakeSnackbarSender,
        shareHelper = FakeShareHelper,
        preference = preference,
        tracker = tracker,
    )
}

private suspend fun EventFlow<Unit>.awaitUnconsumedEvent() {
    assertEquals(Unit, mapNotNull { it.consume() }.first())
}

val fakeSpeakToRecordVm = SpeakToRecordViewModel(
    snackbarSender = FakeSnackbarSender,
    speakToRecord = object : SpeakToRecord {
        override fun startRecording(): RecordActor = RecordActor(
            statusFlow = emptyFlow(),
            stopRecording = {}
        )
    },
    bubbleRepo = FakeBubbleRepo()
)