package com.kevlina.budgetplus.ads

import com.kevlina.budgetplus.core.ads.AdmobInitializer
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.Logger
import com.kevlina.budgetplus.core.common.Tracker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import platform.AppTrackingTransparency.ATTrackingManager
import platform.AppTrackingTransparency.ATTrackingManagerAuthorizationStatusAuthorized
import platform.AppTrackingTransparency.ATTrackingManagerAuthorizationStatusNotDetermined
import swiftPMImport.budgetplus.core.core.ads.GADAdapterStatus
import swiftPMImport.budgetplus.core.core.ads.GADMobileAds

@ContributesIntoSet(AppScope::class, binding = binding<AppStartAction>())
@ContributesBinding(AppScope::class, binding = binding<AdmobInitializer>())
internal class AdmobInitializerImpl(
    private val tracker: Tracker,
) : AppStartAction, AdmobInitializer {

    private val isTrackingDetermined
        get() = ATTrackingManager.trackingAuthorizationStatus != ATTrackingManagerAuthorizationStatusNotDetermined

    private val isTrackingAuthorized
        get() = ATTrackingManager.trackingAuthorizationStatus == ATTrackingManagerAuthorizationStatusAuthorized

    private fun initAdmob() {
        // Meta requires the advertiser tracking flag to reflect the real ATT result,
        // and it must be set before AdMob (and therefore the Meta adapter) starts.
        // Otherwise Meta drops bidding requests.
        MetaAdvertiserTracking.setAdvertiserTrackingEnabled(isTrackingAuthorized)
        GADMobileAds.sharedInstance().startWithCompletionHandler { status ->
            val adapterStatuses = status?.adapterStatusesByClassName.orEmpty()
            adapterStatuses.forEach { (className, adapterStatus) ->
                if (adapterStatus is GADAdapterStatus) {
                    Logger.d(
                        "AdMob adapter: $className, " +
                            "state=${adapterStatus.state}, " +
                            "description=${adapterStatus.description}, " +
                            "latency=${adapterStatus.latency}"
                    )
                }
            }
        }
    }

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
}
