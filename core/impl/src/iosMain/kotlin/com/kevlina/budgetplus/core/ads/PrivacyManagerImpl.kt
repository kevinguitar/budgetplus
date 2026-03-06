package com.kevlina.budgetplus.core.ads

import app.lexilabs.basic.ads.BasicAds
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.Tracker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import platform.AppTrackingTransparency.ATTrackingManager
import platform.AppTrackingTransparency.ATTrackingManagerAuthorizationStatusAuthorized
import platform.AppTrackingTransparency.ATTrackingManagerAuthorizationStatusNotDetermined

@ContributesIntoSet(AppScope::class)
class AdmobInitializer(
    private val tracker: Tracker,
) : AppStartAction {

    override fun onAppStart() {
        // Only trigger if the status hasn't been decided yet
        if (ATTrackingManager.trackingAuthorizationStatus == ATTrackingManagerAuthorizationStatusNotDetermined) {
            ATTrackingManager.requestTrackingAuthorizationWithCompletionHandler { status ->
                when (status) {
                    ATTrackingManagerAuthorizationStatusAuthorized -> {
                        tracker.logEvent("tracking_permission_granted")
                    }

                    else -> {
                        tracker.logEvent("tracking_permission_denied")
                    }
                }
                initAdmob()
            }
        } else {
            initAdmob()
        }
    }

    private fun initAdmob() {
        @Suppress("DEPRECATION")
        BasicAds.initialize(null)
    }
}