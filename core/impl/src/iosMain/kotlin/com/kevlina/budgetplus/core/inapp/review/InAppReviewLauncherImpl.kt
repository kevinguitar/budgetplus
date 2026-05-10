package com.kevlina.budgetplus.core.inapp.review

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindowScene

@ContributesBinding(AppScope::class)
internal class InAppReviewLauncherImpl : InAppReviewLauncher {

    override suspend fun launchReviewFlow() {
        val scene = UIApplication.sharedApplication.connectedScenes
            .mapNotNull { it as? UIWindowScene }
            .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }

        if (scene != null) {
            SKStoreReviewController.requestReviewInScene(scene)
        } else {
            SKStoreReviewController.requestReview()
        }
    }
}