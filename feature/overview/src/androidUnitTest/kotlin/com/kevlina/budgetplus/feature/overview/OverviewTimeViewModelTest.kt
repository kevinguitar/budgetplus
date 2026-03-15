package com.kevlina.budgetplus.feature.overview

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.overview_exceed_max_period
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.data.BookRepo
import com.kevlina.budgetplus.core.data.RecordsObserver
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.remote.Book
import com.kevlina.budgetplus.core.data.remote.TimePeriod
import com.kevlina.budgetplus.core.unit.test.MainDispatcherRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.Rule
import org.junit.Test

class OverviewTimeViewModelTest {

    @get:Rule
    val rule = MainDispatcherRule()

    @Test
    fun `setting the period by clicking on previous day`() = runTest {
        every { recordsObserver.timePeriod } returns flowOf(oneDayPeriod)

        val model = createModel()
        model.previousDay()

        val yesterday = LocalDate.now().minus(1, DateTimeUnit.DAY)
        verify {
            recordsObserver.setTimePeriod(bookId, TimePeriod.Custom(yesterday, yesterday))
        }
    }

    @Test
    fun `setting the period by clicking on next day`() = runTest {
        every { recordsObserver.timePeriod } returns flowOf(oneDayPeriod)

        val model = createModel()
        model.nextDay()

        val tomorrow = LocalDate.now().plus(1, DateTimeUnit.DAY)
        verify {
            recordsObserver.setTimePeriod(bookId, TimePeriod.Custom(tomorrow, tomorrow))
        }
    }

    @Test
    fun `WHEN the period is more than one month THEN make it one month`() = runTest {
        every { recordsObserver.timePeriod } returns flowOf(oneDayPeriod)

        val model = createModel()
        model.setTimePeriod(TimePeriod.Custom(
            from = LocalDate.now(),
            until = LocalDate.now().plus(3, DateTimeUnit.MONTH)
        ))

        verify {
            FakeSnackbarSender.lastSentMessageRes = Res.string.overview_exceed_max_period
            recordsObserver.setTimePeriod(bookId, TimePeriod.Custom(
                from = LocalDate.now(),
                until = LocalDate.now().plus(1, DateTimeUnit.MONTH)
            ))
        }
    }

    @Test
    fun `setting the custom period saves it to preference`() = runTest {
        val fakePreference = FakePreference()
        val bookRepo = FakeBookRepo(currentBookId = bookId, book = Book(id = bookId))
        every { recordsObserver.timePeriod } returns flowOf(oneDayPeriod)

        val model = createModel(bookRepo = bookRepo, preference = fakePreference)

        val customPeriod = TimePeriod.Custom(
            from = LocalDate.now(),
            until = LocalDate.now().plus(1, DateTimeUnit.WEEK)
        )
        model.setTimePeriod(customPeriod, isCustomized = true)

        assert(model.customPeriod.value == customPeriod)
    }

    @Test
    fun `changing book updates custom period`() = runTest {
        val fakePreference = FakePreference()
        every { recordsObserver.timePeriod } returns flowOf(oneDayPeriod)

        val bookRepo1 = FakeBookRepo(currentBookId = bookId, book = Book(id = bookId))
        val model1 = createModel(bookRepo = bookRepo1, preference = fakePreference)

        val customPeriod1 = TimePeriod.Custom(
            from = LocalDate.now(),
            until = LocalDate.now().plus(1, DateTimeUnit.WEEK)
        )

        model1.setTimePeriod(customPeriod1, isCustomized = true)
        assert(model1.customPeriod.value == customPeriod1)

        // Switch to another book by creating a new model with a different bookId
        val bookId2 = "another_book"
        val bookRepo2 = FakeBookRepo(currentBookId = bookId2, book = Book(id = bookId2))
        val model2 = createModel(bookRepo = bookRepo2, preference = fakePreference)

        // Initially null for the new book
        assert(model2.customPeriod.value == null)

        // Set custom period for the new book
        val customPeriod2 = TimePeriod.Custom(
            from = LocalDate.now(),
            until = LocalDate.now().plus(2, DateTimeUnit.WEEK)
        )
        model2.setTimePeriod(customPeriod2, isCustomized = true)
        assert(model2.customPeriod.value == customPeriod2)

        // Switch back to the first book
        val model3 = createModel(bookRepo = bookRepo1, preference = fakePreference)
        assert(model3.customPeriod.value == customPeriod1)
    }

    @Test
    fun `setting the date range with customization saves it to preference`() = runTest {
        val fakePreference = FakePreference()
        val bookRepo = FakeBookRepo(currentBookId = bookId, book = Book(id = bookId))
        every { recordsObserver.timePeriod } returns flowOf(oneDayPeriod)

        val model = createModel(bookRepo = bookRepo, preference = fakePreference)

        val from = LocalDate.now()
        val until = LocalDate.now().plus(3, DateTimeUnit.DAY)
        model.setDateRange(from, until, isCustomized = true)

        assert(model.customPeriod.value == TimePeriod.Custom(from, until))
    }

    private val bookId = "my_book"
    private val oneDayPeriod = TimePeriod.Custom(LocalDate.now(), LocalDate.now())

    private val recordsObserver = mockk<RecordsObserver>()

    private fun TestScope.createModel(
        bookRepo: BookRepo = FakeBookRepo(currentBookId = bookId, book = Book(id = bookId)),
        preference: FakePreference = FakePreference(),
    ): OverviewTimeViewModel {
        every { recordsObserver.setTimePeriod(any(), any()) } just runs

        val model = OverviewTimeViewModel(
            recordsObserver = recordsObserver,
            bookRepo = bookRepo,
            authManager = FakeAuthManager(),
            snackbarSender = FakeSnackbarSender,
            tracker = FakeTracker(),
            preference = preference
        )
        backgroundScope.launch(rule.testDispatcher) {
            model.timePeriod.collect()
        }
        backgroundScope.launch(rule.testDispatcher) {
            model.customPeriod.collect()
        }
        return model
    }
}