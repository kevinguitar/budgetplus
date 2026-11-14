package com.kevlina.budgetplus.ads

import com.kevlina.budgetplus.core.ads.AdmobInitializer
import com.kevlina.budgetplus.core.ads.AdsAvailability
import com.kevlina.budgetplus.core.ads.AdsConsentManager
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.Logger
import com.kevlina.budgetplus.core.common.Tracker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AppTrackingTransparency.ATTrackingManager
import platform.AppTrackingTransparency.ATTrackingManagerAuthorizationStatusAuthorized
import platform.AppTrackingTransparency.ATTrackingManagerAuthorizationStatusNotDetermined
import swiftPMImport.budgetplus.core.core.ads.GADAdapterStatus
import swiftPMImport.budgetplus.core.core.ads.GADMobileAds
import kotlin.coroutines.resume

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<AppStartAction>())
@ContributesBinding(AppScope::class, binding = binding<AdmobInitializer>())
internal class AdmobInitializerImpl(
    private val adsConsentManager: AdsConsentManager,
    private val tracker: Tracker,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : AppStartAction, AdmobInitializer {

    override val adsAvailability: StateFlow<AdsAvailability>
        field = MutableStateFlow(AdsAvailability.Pending)

    // Only accessed on the main thread through appScope.
    private var isAdmobInitialized = false

    private val isTrackingDetermined
        get() = ATTrackingManager.trackingAuthorizationStatus != ATTrackingManagerAuthorizationStatusNotDetermined

    private val isTrackingAuthorized
        get() = ATTrackingManager.trackingAuthorizationStatus == ATTrackingManagerAuthorizationStatusAuthorized

    private fun initAdmob() {
        if (isAdmobInitialized) return
        isAdmobInitialized = true

        // Meta requires the advertiser tracking flag to reflect the real ATT result,
        // and it must be set before AdMob (and therefore the Meta adapter) starts.
        // Otherwise Meta drops bidding requests.
        MetaAdvertiserTracking.setAdvertiserTrackingEnabled(isTrackingAuthorized)
        Logger.d("AdMob: Starting the Mobile Ads SDK, trackingAuthorized=$isTrackingAuthorized")
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
        adsAvailability.value = AdsAvailability.Available
    }

    override fun onAppStart() {
        // Start the SDK as soon as ads can be requested. For the returning users, it's right after
        // the consent info update is sent, with the consent from the previous session. For the
        // new users who don't require consent, it's when the update response arrives.
        // Otherwise, it waits for the consent and ATT to be gathered in requestConsent().
        appScope.launch {
            adsConsentManager.canRequestAds.first { it }
            if (isTrackingDetermined) {
                initAdmob()
            }
        }
        // Refresh the consent info on every app launch, without showing the form.
        appScope.launch {
            adsConsentManager.updateConsentInfo()
        }
    }

    override fun requestConsent() {
        appScope.launch {
            val wasTrackingDetermined = isTrackingDetermined

            // Gather the GDPR consent first. When an IDFA explainer message is configured in
            // the AdMob console, UMP also shows the ATT prompt as a part of its flow.
            val canRequestAds = adsConsentManager.gatherConsent()

            // Only trigger if the status hasn't been decided yet, and ads can be requested.
            if (canRequestAds && !isTrackingDetermined) {
                requestTrackingAuthorization()
            }
            // The ATT prompt is either shown by UMP or by us, track the result in both cases.
            if (!wasTrackingDetermined && isTrackingDetermined) {
                Logger.d("AdMob: ATT determined, authorized=$isTrackingAuthorized")
                tracker.logEvent(
                    if (isTrackingAuthorized) "tracking_permission_granted" else "tracking_permission_denied"
                )
            }

            if (canRequestAds) {
                initAdmob()
            } else if (adsAvailability.value == AdsAvailability.Pending) {
                adsAvailability.value = AdsAvailability.Unavailable
            }
        }
    }

    private suspend fun requestTrackingAuthorization() = suspendCancellableCoroutine { cont ->
        ATTrackingManager.requestTrackingAuthorizationWithCompletionHandler {
            cont.resume(Unit)
        }
    }
}
