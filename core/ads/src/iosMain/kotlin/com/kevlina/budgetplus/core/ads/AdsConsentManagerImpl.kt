package com.kevlina.budgetplus.core.ads

import com.kevlina.budgetplus.core.common.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import swiftPMImport.budgetplus.core.core.ads.UMPConsentForm
import swiftPMImport.budgetplus.core.core.ads.UMPConsentInformation
import swiftPMImport.budgetplus.core.core.ads.UMPPrivacyOptionsRequirementStatusRequired
import swiftPMImport.budgetplus.core.core.ads.UMPRequestParameters
import kotlin.coroutines.resume

/**
 * https://developers.google.com/admob/ios/privacy
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class AdsConsentManagerImpl : AdsConsentManager {

    private val consentInformation: UMPConsentInformation
        get() = UMPConsentInformation.sharedInstance

    private val mutex = Mutex()
    private var isConsentInfoUpdated = false
    private var isConsentGathered = false

    override val canRequestAds: StateFlow<Boolean>
        field = MutableStateFlow(false)

    override val isPrivacyOptionsRequired: StateFlow<Boolean>
        field = MutableStateFlow(false)

    override suspend fun updateConsentInfo() {
        mutex.withLock { updateConsentInfoIfNeeded() }
    }

    override suspend fun gatherConsent(): Boolean = mutex.withLock {
        updateConsentInfoIfNeeded()
        if (!isConsentGathered) {
            isConsentGathered = loadAndPresentConsentFormIfRequired()
            refreshState("Consent form handled=$isConsentGathered")
        }
        canRequestAds.value
    }

    override fun showPrivacyOptionsForm() {
        UMPConsentForm.presentPrivacyOptionsFormFromViewController(topViewController()) { error ->
            if (error != null) {
                Logger.w("AdsConsent: Failed to show the privacy options form. ${error.localizedDescription}")
            }
            refreshState("Privacy options form dismissed")
        }
    }

    private suspend fun updateConsentInfoIfNeeded() {
        if (isConsentInfoUpdated) return

        isConsentInfoUpdated = requestConsentInfoUpdate()
        refreshState("Consent info updated=$isConsentInfoUpdated")
    }

    private fun refreshState(reason: String) {
        canRequestAds.value = consentInformation.canRequestAds
        isPrivacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus == UMPPrivacyOptionsRequirementStatusRequired
        Logger.d(
            "AdsConsent: $reason, " +
                "consentStatus=${consentInformation.consentStatus}, " +
                "canRequestAds=${canRequestAds.value}, " +
                "privacyOptionsRequired=${isPrivacyOptionsRequired.value}"
        )
    }

    /**
     * @return whether the consent info is updated successfully.
     */
    private suspend fun requestConsentInfoUpdate(): Boolean = suspendCancellableCoroutine { cont ->
        consentInformation.requestConsentInfoUpdateWithParameters(UMPRequestParameters()) { error ->
            if (error != null) {
                // The consent gathered in the previous sessions still applies, see canRequestAds.
                Logger.w("AdsConsent: Failed to update the consent info. ${error.localizedDescription}")
            }
            cont.resume(error == null)
        }
        // Once the update is requested, canRequestAds reflects the consent from the previous
        // session, without waiting for the response. This lets returning users start the
        // Mobile Ads SDK right away.
        refreshState("Consent info update requested")
    }

    /**
     * @return whether the consent form is handled, i.e. shown or not required.
     */
    private suspend fun loadAndPresentConsentFormIfRequired(): Boolean = suspendCancellableCoroutine { cont ->
        UMPConsentForm.loadAndPresentIfRequiredFromViewController(topViewController()) { error ->
            if (error != null) {
                Logger.w("AdsConsent: Failed to show the consent form. ${error.localizedDescription}")
            }
            cont.resume(error == null)
        }
    }

    /**
     * The forms must be presented from the topmost view controller, presenting from a view
     * controller which already presents another one fails.
     */
    private fun topViewController(): UIViewController? {
        var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (controller?.presentedViewController != null) {
            controller = controller.presentedViewController
        }
        return controller
    }
}
