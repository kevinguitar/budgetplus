package com.kevlina.budgetplus.core.data.remote

import com.google.common.truth.Truth.assertThat
import com.kevlina.budgetplus.core.common.now
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.junit.Test

class TimePeriodTest {

    @Test
    fun `WHEN Today is selected THEN from is equal to until`() {
        val period = TimePeriod.Today
        assertThat(period.from).isEqualTo(LocalDate.now())
        assertThat(period.from).isEqualTo(period.until)
    }

    @Test
    fun `WHEN Week is selected THEN from is 7 days earlier than until`() {
        val period = TimePeriod.Week
        assertThat(period.from > LocalDate.now()).isFalse()
        assertThat(period.from.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
        assertThat(period.until.dayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
        assertThat(period.from.plus(6, DateTimeUnit.DAY)).isEqualTo(period.until)
    }

    @Test
    fun `WHEN Month is selected THEN from will be the first day of the month`() {
        val period = TimePeriod.Month
        assertThat(period.from > LocalDate.now()).isFalse()
        assertThat(period.from.month).isEqualTo(period.until.month)
        assertThat(period.from.day).isEqualTo(1)
        assertThat(period.from.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY))
            .isEqualTo(period.until)
    }

    @Test
    fun `WHEN LastMonth is selected THEN until will be earlier than now`() {
        val period = TimePeriod.LastMonth
        assertThat(period.until < LocalDate.now()).isTrue()
        assertThat(period.from.month).isEqualTo(period.until.month)
        assertThat(period.from.day).isEqualTo(1)
        assertThat(period.from.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY))
            .isEqualTo(period.until)
    }

    @Test
    fun `WHEN Custom is selected and until is earlier than from THEN throw exception`() {
        try {
            TimePeriod.Custom(
                from = LocalDate.now().plus(1, DateTimeUnit.DAY),
                until = LocalDate.now()
            )
        } catch (e: Exception) {
            assertThat(e.message).isEqualTo("From date is later than until.")
        }
    }

    @Test
    fun `WHEN Custom is selected and until is later than from THEN no error`() {
        val period = TimePeriod.Custom(
            from = LocalDate.now(),
            until = LocalDate.now().plus(1, DateTimeUnit.DAY)
        )
        assertThat(period.from < (period.until)).isTrue()
    }
}