package com.kevlina.budgetplus.core.inapp.review

import android.content.Context
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.kevlina.budgetplus.core.common.ActivityProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding

@ContributesBinding(AppScope::class)
internal class InAppReviewLauncherImpl(
    private val activityProvider: ActivityProvider,
    private val context: Context,
) : InAppReviewLauncher {

    override suspend fun launchReviewFlow() {
        val activity = activityProvider.currentActivity ?: return
        val reviewManager = ReviewManagerFactory.create(context)
        val reviewInfo = reviewManager.requestReview()
        reviewManager.launchReview(activity, reviewInfo)
    }
}