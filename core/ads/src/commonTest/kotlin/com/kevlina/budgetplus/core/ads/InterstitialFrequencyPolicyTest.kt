package com.kevlina.budgetplus.core.ads

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class InterstitialFrequencyPolicyTest {

    private val timeZone = TimeZone.UTC
    // 2026-11-21T12:00:00Z
    private val now = Instant.parse("2026-11-21T12:00:00Z")
    private val today = LocalDate(2026, 11, 21)
    private val oldUser = now - 30.days

    private val frequency = InterstitialFrequency(
        minInterval = 30.minutes,
        dailyCap = 2,
        newUserGracePeriod = 3.days,
    )

    private fun skipReason(
        history: InterstitialHistory = InterstitialHistory(),
        userCreatedOn: Instant? = oldUser,
        at: Instant = now,
    ) = InterstitialFrequencyPolicy.skipReason(frequency, history, userCreatedOn, at, timeZone)

    @Test
    fun `show the ad when it was never shown`() {
        assertNull(skipReason())
    }

    @Test
    fun `skip during the new user grace period`() {
        assertEquals(InterstitialSkipReason.NewUser, skipReason(userCreatedOn = now - 2.days))
    }

    @Test
    fun `show the ad once the grace period is over`() {
        assertNull(skipReason(userCreatedOn = now - 3.days))
    }

    @Test
    fun `unknown sign up time is not treated as a new user`() {
        assertNull(skipReason(userCreatedOn = null))
    }

    @Test
    fun `skip when the last ad was shown within the min interval`() {
        val history = InterstitialHistory(
            lastShownAtMillis = (now - 29.minutes).toEpochMilliseconds(),
            shownOn = today,
            shownCountOnDay = 1,
        )
        assertEquals(InterstitialSkipReason.TooSoon, skipReason(history))
    }

    @Test
    fun `show when the min interval passed and the daily cap isn't reached`() {
        val history = InterstitialHistory(
            lastShownAtMillis = (now - 30.minutes).toEpochMilliseconds(),
            shownOn = today,
            shownCountOnDay = 1,
        )
        assertNull(skipReason(history))
    }

    @Test
    fun `skip when the daily cap is reached`() {
        val history = InterstitialHistory(
            lastShownAtMillis = (now - 2.hours).toEpochMilliseconds(),
            shownOn = today,
            shownCountOnDay = 2,
        )
        assertEquals(InterstitialSkipReason.DailyCapReached, skipReason(history))
    }

    @Test
    fun `the daily cap resets on the next day`() {
        val history = InterstitialHistory(
            lastShownAtMillis = (now - 1.days).toEpochMilliseconds(),
            shownOn = LocalDate(2026, 11, 20),
            shownCountOnDay = 2,
        )
        assertNull(skipReason(history))
    }

    @Test
    fun `ignore a last shown time in the future`() {
        val history = InterstitialHistory(lastShownAtMillis = (now + 1.hours).toEpochMilliseconds())
        assertNull(skipReason(history))
    }

    @Test
    fun `zero values mean no frequency limits, the legacy behavior`() {
        val result = InterstitialFrequencyPolicy.skipReason(
            frequency = InterstitialFrequency(
                minInterval = 0.minutes,
                dailyCap = 0,
                newUserGracePeriod = 0.days,
            ),
            history = InterstitialHistory(
                lastShownAtMillis = now.toEpochMilliseconds(),
                shownOn = today,
                shownCountOnDay = 100,
            ),
            userCreatedOn = now,
            now = now,
            timeZone = timeZone,
        )
        assertNull(result)
    }

    @Test
    fun `record shown increments the count of the same day`() {
        val history = InterstitialHistory()
            .recordShown(now - 1.hours, timeZone)
            .recordShown(now, timeZone)

        assertEquals(now.toEpochMilliseconds(), history.lastShownAtMillis)
        assertEquals(2, history.shownCountOn(today))
    }

    @Test
    fun `record shown restarts the count on a new day`() {
        val history = InterstitialHistory(shownOn = LocalDate(2026, 11, 20), shownCountOnDay = 2)
            .recordShown(now, timeZone)

        assertEquals(1, history.shownCountOn(today))
    }

    @Test
    fun `isNewUser respects the grace period`() {
        assertTrue(InterstitialFrequencyPolicy.isNewUser(frequency, now - 1.days, now))
        assertFalse(InterstitialFrequencyPolicy.isNewUser(frequency, now - 4.days, now))
    }
}
