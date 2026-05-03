package com.kevlina.budgetplus.inapp.review.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.inapp.review.InAppReviewManager

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeInAppReviewManager(
    private val isEligibleForReview: Boolean = true,
) : InAppReviewManager {

    var hasLaunchedReviewFlow = false
    var hasRejectedReviewing = false

    override suspend fun isEligibleForReview(): Boolean = isEligibleForReview

    override suspend fun launchReviewFlow() {
        hasLaunchedReviewFlow = true
    }

    override fun rejectReviewing() {
        hasRejectedReviewing = true
    }
}