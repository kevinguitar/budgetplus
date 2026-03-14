package com.kevlina.budgetplus.feature.overview

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.overview_exceed_max_period
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.data.RecordsObserver
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakeBookRepo
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

    private val bookId = "my_book"
    private val oneDayPeriod = TimePeriod.Custom(LocalDate.now(), LocalDate.now())

    private val recordsObserver = mockk<RecordsObserver> {
        every { setTimePeriod(bookId, any()) } just runs
    }

    private fun TestScope.createModel() = OverviewTimeViewModel(
        recordsObserver = recordsObserver,
        bookRepo = FakeBookRepo(currentBookId = bookId),
        authManager = FakeAuthManager(),
        snackbarSender = FakeSnackbarSender,
        tracker = FakeTracker(),
    ).apply {
        backgroundScope.launch(rule.testDispatcher) {
            timePeriod.collect()
        }
    }
}