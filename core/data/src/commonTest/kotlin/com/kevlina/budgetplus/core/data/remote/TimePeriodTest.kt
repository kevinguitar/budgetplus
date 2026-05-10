package com.kevlina.budgetplus.core.data.remote

import com.kevlina.budgetplus.core.common.now
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimePeriodTest {

    @Test
    fun `WHEN Today is selected THEN from is equal to until`() {
        val period = TimePeriod.Today
        assertEquals(LocalDate.now(), period.from)
        assertEquals(period.until, period.from)
    }

    @Test
    fun `WHEN Week is selected THEN from is 7 days earlier than until`() {
        val period = TimePeriod.Week
        assertFalse(period.from > LocalDate.now())
        assertEquals(DayOfWeek.MONDAY, period.from.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, period.until.dayOfWeek)
        assertEquals(period.until, period.from.plus(6, DateTimeUnit.DAY))
    }

    @Test
    fun `WHEN Month is selected THEN from will be the first day of the month`() {
        val period = TimePeriod.Month
        assertFalse(period.from > LocalDate.now())
        assertEquals(period.until.month, period.from.month)
        assertEquals(1, period.from.day)
        assertEquals(
            period.until,
            period.from.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        )
    }

    @Test
    fun `WHEN LastMonth is selected THEN until will be earlier than now`() {
        val period = TimePeriod.LastMonth
        assertTrue(period.until < LocalDate.now())
        assertEquals(period.until.month, period.from.month)
        assertEquals(1, period.from.day)
        assertEquals(
            period.until,
            period.from.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        )
    }

    @Test
    fun `WHEN Custom is selected and until is earlier than from THEN throw exception`() {
        try {
            TimePeriod.Custom(
                from = LocalDate.now().plus(1, DateTimeUnit.DAY),
                until = LocalDate.now()
            )
        } catch (e: Exception) {
            assertEquals("From date is later than until.", e.message)
        }
    }

    @Test
    fun `WHEN Custom is selected and until is later than from THEN no error`() {
        val period = TimePeriod.Custom(
            from = LocalDate.now(),
            until = LocalDate.now().plus(1, DateTimeUnit.DAY)
        )
        assertTrue(period.from < period.until)
    }
}
