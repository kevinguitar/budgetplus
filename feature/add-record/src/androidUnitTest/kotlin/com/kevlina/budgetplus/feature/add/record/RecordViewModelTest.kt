package com.kevlina.budgetplus.feature.add.record

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.datastore.preferences.core.intPreferencesKey
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.record_empty_category
import budgetplus.core.common.generated.resources.record_empty_price
import com.google.common.truth.Truth.assertThat
import com.kevlina.budgetplus.core.ads.fixtures.FakeInterstitialAdsHandler
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.ExpressionEvaluator
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.fixtures.FakeShareHelper
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
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
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.ui.bubble.FakeBubbleRepo
import com.kevlina.budgetplus.core.unit.test.MainDispatcherRule
import com.kevlina.budgetplus.feature.category.pills.CategoriesViewModel
import com.kevlina.budgetplus.feature.freeze.fakeFreezeBookVm
import com.kevlina.budgetplus.feature.speak.record.RecordActor
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecord
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecordViewModel
import com.kevlina.budgetplus.inapp.review.fixtures.FakeInAppReviewManager
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Rule
import org.junit.Test

class RecordViewModelTest {

    @get:Rule
    val rule = MainDispatcherRule()

    @Test
    fun `show message when category is empty`() = runTest {
        createModel()
        categoriesVm.setCategory(null)
        calculatorVm.input("1")
        calculatorVm.evaluate()

        assertThat(FakeSnackbarSender.lastSentMessageRes).isEqualTo(Res.string.record_empty_category)
    }

    @Test
    fun `show message when price is empty`() = runTest {
        createModel()
        calculatorVm.evaluate()

        assertThat(FakeSnackbarSender.lastSentMessageRes).isEqualTo(Res.string.record_empty_price)
    }

    @Test
    fun `record should be created with correct info in RecordRepo`() = runTest {
        val date = LocalDate.now().minus(1, DateTimeUnit.DAY)

        createModel().apply {
            setDate(date)
            note.setTextAndPlaceCursorAtEnd("Test note")
            setType(RecordType.Income)
        }

        calculatorVm.input("123")
        calculatorVm.evaluate()

        assertThat(
            // Do not verify timestamp as it depends on test execution time
            FakeRecordRepo.lastCreatedRecord?.copy(timestamp = null)
        ).isEqualTo(
            Record(
                type = RecordType.Income,
                category = "Test category",
                name = "Test note",
                date = date.toEpochDays(),
                price = 123.0,
            )
        )
    }

    @Test
    fun `record without note should fallback to category`() = runTest {
        createModel()

        calculatorVm.input("1.23")
        calculatorVm.evaluate()

        assertThat(
            // Do not verify timestamp as it depends on test execution time
            FakeRecordRepo.lastCreatedRecord?.copy(timestamp = null)
        ).isEqualTo(
            Record(
                type = RecordType.Expense,
                category = "Test category",
                name = "Test category",
                date = LocalDate.now().toEpochDays(),
                price = 1.23,
            )
        )
    }

    @Test
    fun `reset screen after the record is recorded`() = runTest {
        val model = createModel()
        model.note.setTextAndPlaceCursorAtEnd("Test note")

        calculatorVm.input("1")
        calculatorVm.evaluate()

        assertThat(categoriesVm.category.value).isNull()
        assertThat(model.note.text.toString()).isEmpty()
        assertThat(calculatorVm.priceText.text).isEqualTo("0")
    }

    @Test
    fun `show fullscreen ad on every 7th record`() = runTest {
        createModel(recordCount = 6)
        calculatorVm.input("1")
        calculatorVm.evaluate()

        assertThat(interstitialAdsHandler.count).isEqualTo(1)
    }

    @Test
    fun `request notification permission after the 2nd record`() = runTest {
        val model = createModel(recordCount = 1)
        calculatorVm.input("1")
        calculatorVm.evaluate()

        model.requestPermissionEvent.awaitUnconsumedEvent()
    }

    @Test
    fun `request in app review after 4th record`() = runTest {
        val model = createModel(recordCount = 3)
        calculatorVm.input("1")
        calculatorVm.evaluate()

        model.requestReviewEvent.awaitUnconsumedEvent()
    }

    @Test
    fun `editCurrency should navigate to BookCurrency when user can edit book`() = runTest {
        val model = createModel(canEditBook = true)
        model.editCurrency()

        assertThat(model.navController.backStack.last()).isEqualTo(
            BookDest.CurrencyPicker(purpose = BookDest.CurrencyPicker.Purpose.Book)
        )
    }

    @Test
    fun `editCurrency should not navigate when user cannot edit book`() = runTest {
        val model = createModel(canEditBook = false)
        val initialDest = model.navController.backStack.last()
        model.editCurrency()

        assertThat(model.navController.backStack.last()).isEqualTo(initialDest)
    }

    @Test
    fun `editPreferredCurrency should navigate to PreferredCurrency when user is premium`() = runTest {
        val model = createModel(isPremium = true)
        model.editPreferredCurrency()

        assertThat(model.navController.backStack.last()).isEqualTo(
            BookDest.CurrencyPicker(purpose = BookDest.CurrencyPicker.Purpose.Preferred)
        )
    }

    @Test
    fun `editPreferredCurrency should navigate to UnlockPremium when user is not premium`() = runTest {
        val model = createModel(isPremium = false)
        model.editPreferredCurrency()

        assertThat(model.navController.backStack.last()).isEqualTo(BookDest.UnlockPremium)
    }

    @Test
    fun `preferredCurrencyPrice should emit formatted price from currencyExchangeRepo`() = runTest {
        val currencyExchangeRepo = FakeCurrencyExchangeRepo(preferredCurrencyCode = "EUR")
        val model = createModel(currencyExchangeRepo = currencyExchangeRepo)

        calculatorVm.input("100")
        assertThat(model.preferredCurrencyPrice.first { it != null }).isEqualTo("100.0 EUR")
    }


    private val calculatorVm = CalculatorViewModel(
        vibrator = FakeVibratorManager(),
        snackbarSender = FakeSnackbarSender,
        speakToRecordVm = fakeSpeakToRecordVm,
        freezeBookVm = fakeFreezeBookVm,
        expressionEvaluator = ExpressionEvaluator(),
    )

    private val bookRepo = FakeBookRepo()
    private val categoriesVm = CategoriesViewModel(bookRepo = bookRepo).apply {
        setCategory("Test category")
    }

    private val interstitialAdsHandler = FakeInterstitialAdsHandler()

    private fun createModel(
        recordCount: Int = 0,
        canEditBook: Boolean = true,
        isPremium: Boolean = false,
        currencyExchangeRepo: CurrencyExchangeRepo = FakeCurrencyExchangeRepo(),
    ) = RecordViewModel(
        calculatorVm = calculatorVm,
        categoriesVm = categoriesVm,
        freezeBookVm = fakeFreezeBookVm,
        bookRepo = FakeBookRepo(canEdit = canEditBook),
        navController = NavController(startRoot = BookDest.Record),
        recordRepo = FakeRecordRepo,
        bubbleRepo = FakeBubbleRepo(),
        authManager = FakeAuthManager(isPremium = isPremium),
        interstitialAdsHandler = interstitialAdsHandler,
        inAppReviewManager = FakeInAppReviewManager(),
        currencyExchangeRepo = currencyExchangeRepo,
        snackbarSender = FakeSnackbarSender,
        shareHelper = FakeShareHelper,
        preference = FakePreference {
            set(intPreferencesKey("recordCount"), recordCount)
        },
    )
}

private suspend fun EventFlow<Unit>.awaitUnconsumedEvent() {
    assertThat(mapNotNull { it.consume() }.first()).isEqualTo(Unit)
}

val fakeSpeakToRecordVm = SpeakToRecordViewModel(
    snackbarSender = FakeSnackbarSender,
    speakToRecord = object : SpeakToRecord {
        override val isAvailableOnDevice: Boolean
            get() = true

        override fun startRecording(): RecordActor = RecordActor(
            statusFlow = emptyFlow(),
            stopRecording = {}
        )
    },
    bubbleRepo = FakeBubbleRepo()
)