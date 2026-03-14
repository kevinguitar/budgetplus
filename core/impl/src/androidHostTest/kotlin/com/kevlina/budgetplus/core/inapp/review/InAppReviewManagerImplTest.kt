package com.kevlina.budgetplus.core.inapp.review

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.kevlina.budgetplus.core.common.fixtures.FakeSnackbarSender
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class InAppReviewManagerImplTest {

    @Test
    fun `WHEN the app is fresh install THEN is not eligible for review`() = runTest {
        val reviewManager = createReviewManager(firstInitDatetime = ineligibleTime)
        assertThat(reviewManager.isEligibleForReview()).isFalse()
    }

    @Test
    fun `WHEN the user already rejected before THEN is not eligible for review`() = runTest {
        val reviewManager = createReviewManager(hasRejectedBefore = true)
        assertThat(reviewManager.isEligibleForReview()).isFalse()
    }

    @Test
    fun `WHEN we already requested the review before THEN is not eligible for review`() = runTest {
        val reviewManager = createReviewManager(hasRequestedBefore = true)
        assertThat(reviewManager.isEligibleForReview()).isFalse()
    }

    @Test
    fun `WHEN we never requested and the app is installed more than 3 days THEN request review`() = runTest {
        val reviewManager = createReviewManager()
        assertThat(reviewManager.isEligibleForReview()).isTrue()
        assertThat(tracker.lastEventName).isEqualTo("inapp_review_requested")
    }

    private val eligibleTime = (Clock.System.now() - 5.days).epochSeconds
    private val ineligibleTime = Clock.System.now().epochSeconds

    private val tracker = FakeTracker()

    private fun TestScope.createReviewManager(
        firstInitDatetime: Long = eligibleTime,
        hasRejectedBefore: Boolean = false,
        hasRequestedBefore: Boolean = false,
    ) = InAppReviewManagerImpl(
        inAppReviewLauncher = { },
        snackbarSender = FakeSnackbarSender,
        tracker = tracker,
        preference = FakePreference {
            set(longPreferencesKey("firstInitDatetime"), firstInitDatetime)
            set(booleanPreferencesKey("hasRejectedBefore"), hasRejectedBefore)
            set(booleanPreferencesKey("hasRequestedBefore"), hasRequestedBefore)
        },
        appScope = backgroundScope,
    )
}