package com.kevlina.budgetplus.core.ads

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * When the user last saw interstitial ads, persisted to apply the frequency caps across sessions.
 */
@Serializable
data class InterstitialHistory(
    val lastShownAtMillis: Long? = null,
    val shownOn: LocalDate? = null,
    val shownCountOnDay: Int = 0,
) {

    fun shownCountOn(day: LocalDate): Int = if (shownOn == day) shownCountOnDay else 0

    fun recordShown(now: Instant, timeZone: TimeZone): InterstitialHistory {
        val today = now.toLocalDateTime(timeZone).date
        return InterstitialHistory(
            lastShownAtMillis = now.toEpochMilliseconds(),
            shownOn = today,
            shownCountOnDay = shownCountOn(today) + 1,
        )
    }
}

enum class InterstitialSkipReason {
    NewUser, TooSoon, DailyCapReached
}

object InterstitialFrequencyPolicy {

    /**
     * @return why the ad should be skipped, or null if it can be shown.
     */
    fun skipReason(
        frequency: InterstitialFrequency,
        history: InterstitialHistory,
        userCreatedOn: Instant?,
        now: Instant,
        timeZone: TimeZone,
    ): InterstitialSkipReason? {
        if (isNewUser(frequency, userCreatedOn, now)) {
            return InterstitialSkipReason.NewUser
        }

        val lastShownAt = history.lastShownAtMillis?.let(Instant::fromEpochMilliseconds)
        // Ignore a last shown time in the future, the device clock could have been changed.
        if (lastShownAt != null && lastShownAt <= now && now - lastShownAt < frequency.minInterval) {
            return InterstitialSkipReason.TooSoon
        }

        val today = now.toLocalDateTime(timeZone).date
        // A cap of 0 means no cap.
        if (frequency.dailyCap > 0 && history.shownCountOn(today) >= frequency.dailyCap) {
            return InterstitialSkipReason.DailyCapReached
        }

        return null
    }

    fun isNewUser(
        frequency: InterstitialFrequency,
        userCreatedOn: Instant?,
        now: Instant,
    ): Boolean = userCreatedOn != null && now - userCreatedOn < frequency.newUserGracePeriod
}
