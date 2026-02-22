package com.kevlina.budgetplus.feature.add.record

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.datastore.preferences.core.intPreferencesKey
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.record_empty_category
import budgetplus.core.common.generated.resources.record_empty_price
import com.google.common.truth.Truth.assertThat
import com.kevlina.budgetplus.core.ads.InterstitialAdsHandler
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.ExpressionEvaluator
import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.fixtures.FakeShareHelper
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.fixtures.FakeRecordRepo
import com.kevlina.budgetplus.core.data.fixtures.FakeVibratorManager
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.ui.bubble.FakeBubbleRepo
import com.kevlina.budgetplus.core.unit.test.MainDispatcherRule
import com.kevlina.budgetplus.feature.category.pills.CategoriesViewModel
import com.kevlina.budgetplus.inapp.review.fixtures.FakeInAppReviewManager
import io.mockk.mockk
import io.mockk.verify
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

        verify(exactly = 1) { interstitialAdsHandler.showAd() }
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


    private val calculatorVm = CalculatorViewModel(
        vibrator = FakeVibratorManager(),
        snackbarSender = FakeSnackbarSender,
        speakToRecordVm = mockk(relaxed = true),
        expressionEvaluator = ExpressionEvaluator(),
    )

    private val bookRepo = FakeBookRepo()
    private val categoriesVm = CategoriesViewModel(bookRepo = bookRepo).apply {
        setCategory("Test category")
    }

    private val interstitialAdsHandler = mockk<InterstitialAdsHandler>(relaxed = true)

    private fun createModel(
        recordCount: Int = 0,
    ) = RecordViewModel(
        calculatorVm = calculatorVm,
        categoriesVm = categoriesVm,
        bookRepo = bookRepo,
        recordRepo = FakeRecordRepo,
        bubbleRepo = FakeBubbleRepo(),
        authManager = FakeAuthManager(),
        interstitialAdsHandler = interstitialAdsHandler,
        inAppReviewManager = FakeInAppReviewManager(),
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