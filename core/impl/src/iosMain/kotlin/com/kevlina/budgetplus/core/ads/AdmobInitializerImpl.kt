package com.kevlina.budgetplus.core.ads

import GoogleMobileAds.GADMobileAds
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.Tracker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import platform.AppTrackingTransparency.ATTrackingManager
import platform.AppTrackingTransparency.ATTrackingManagerAuthorizationStatusAuthorized
import platform.AppTrackingTransparency.ATTrackingManagerAuthorizationStatusNotDetermined

@ContributesIntoSet(AppScope::class, binding = binding<AppStartAction>())
@ContributesBinding(AppScope::class, binding = binding<AdmobInitializer>())
class AdmobInitializerImpl(
    private val tracker: Tracker,
) : AppStartAction, AdmobInitializer {

    private val isTrackingDetermined
        get() = ATTrackingManager.trackingAuthorizationStatus != ATTrackingManagerAuthorizationStatusNotDetermined

    override fun onAppStart() {
        if (isTrackingDetermined) {
            initAdmob()
        }
    }

    override fun requestTrackingAuthorization() {
        // Only trigger if the status hasn't been decided yet
        if (isTrackingDetermined) return

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
    }

    private fun initAdmob() {
        GADMobileAds.sharedInstance().startWithCompletionHandler(null)
    }
}