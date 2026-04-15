package com.kevlina.budgetplus.core.inapp.review

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.inapp.review.InAppReviewManagerImpl.Companion.INSTALL_DAYS_MIN
import com.kevlina.budgetplus.inapp.review.InAppReviewManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 *  This implementation controls whether we should launch the review flow or not, depends on a
 *  few conditions. We only request the review when the user uses the app for more than
 *  [INSTALL_DAYS_MIN] days, and will only request the review once per app installation, also,
 *  if the user rejected, we'll never prompt the review flow again for them.
 */
@ContributesBinding(AppScope::class)
internal class InAppReviewManagerImpl(
    private val inAppReviewLauncher: InAppReviewLauncher,
    private val snackbarSender: SnackbarSender,
    private val tracker: Tracker,
    private val preference: Preference,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : InAppReviewManager {

    private val now get() = LocalDateTime.now()

    internal val hasRejectedBeforeKey = booleanPreferencesKey("hasRejectedBefore")
    private var hasRejectedBefore = preference.of(hasRejectedBeforeKey)

    internal val hasRequestedBeforeKey = booleanPreferencesKey("hasRequestedBefore")
    private var hasRequestedBefore = preference.of(hasRequestedBeforeKey)

    internal val firstInitDatetimeKey = longPreferencesKey("firstInitDatetime")
    private val firstInitDatetime = preference.of(firstInitDatetimeKey)

    init {
        appScope.launch {
            if (firstInitDatetime.first() == null) {
                val nowSeconds = now.toInstant(TimeZone.UTC).epochSeconds
                preference.update(firstInitDatetimeKey, nowSeconds)
            }
        }
    }

    override suspend fun isEligibleForReview(): Boolean {
        if (hasRejectedBefore.first() == true || hasRequestedBefore.first() == true) {
            return false
        }

        val initDateTime = Instant.fromEpochSeconds(firstInitDatetime.first() ?: 0)
        val eligible = initDateTime.plus(INSTALL_DAYS_MIN.days) < now.toInstant(TimeZone.UTC)
        if (eligible) {
            tracker.logEvent("inapp_review_requested")
        }
        return eligible
    }

    override suspend fun launchReviewFlow() {
        try {
            inAppReviewLauncher.launchReviewFlow()
            tracker.logEvent("inapp_review_accepted")
            preference.update(hasRequestedBeforeKey, true)
        } catch (e: Exception) {
            snackbarSender.sendError(e)
            Logger.e(e) { "Failed to launch the review flow" }
        }
    }

    override fun rejectReviewing() {
        tracker.logEvent("inapp_review_rejected")
        appScope.launch { preference.update(hasRejectedBeforeKey, true) }
    }

    companion object {
        private const val INSTALL_DAYS_MIN: Long = 3L
    }
}