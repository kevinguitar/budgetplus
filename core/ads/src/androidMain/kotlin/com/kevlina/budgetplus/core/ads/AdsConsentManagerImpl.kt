package com.kevlina.budgetplus.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.kevlina.budgetplus.core.common.ActivityProvider
import com.kevlina.budgetplus.core.common.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * https://developers.google.com/admob/android/privacy
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class AdsConsentManagerImpl(
    context: Context,
    private val activityProvider: ActivityProvider,
) : AdsConsentManager {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

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
            isConsentGathered = loadAndShowConsentFormIfRequired(awaitActivity())
            refreshState("Consent form handled=$isConsentGathered")
        }
        canRequestAds.value
    }

    override fun showPrivacyOptionsForm() {
        val activity = activityProvider.currentActivity ?: return
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Logger.w("AdsConsent: Failed to show the privacy options form. ${formError.message}")
            }
            refreshState("Privacy options form dismissed")
        }
    }

    private suspend fun updateConsentInfoIfNeeded() {
        if (isConsentInfoUpdated) return

        isConsentInfoUpdated = requestConsentInfoUpdate(awaitActivity())
        refreshState("Consent info updated=$isConsentInfoUpdated")
    }

    private fun refreshState(reason: String) {
        canRequestAds.value = consentInformation.canRequestAds()
        isPrivacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus == PrivacyOptionsRequirementStatus.REQUIRED
        Logger.d(
            "AdsConsent: $reason, " +
                "consentStatus=${consentInformation.consentStatus}, " +
                "canRequestAds=${canRequestAds.value}, " +
                "privacyOptionsRequired=${isPrivacyOptionsRequired.value}"
        )
    }

    private suspend fun awaitActivity(): Activity {
        return activityProvider.activityFlow.filterNotNull().first()
    }

    /**
     * @return whether the consent info is updated successfully.
     */
    private suspend fun requestConsentInfoUpdate(
        activity: Activity,
    ): Boolean = suspendCancellableCoroutine { cont ->
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            { cont.resume(true) },
            { formError ->
                // The consent gathered in the previous sessions still applies, see canRequestAds.
                Logger.w("AdsConsent: Failed to update the consent info. ${formError.message}")
                cont.resume(false)
            }
        )
        // Once the update is requested, canRequestAds reflects the consent from the previous
        // session, without waiting for the response. This lets returning users start the
        // Mobile Ads SDK right away.
        refreshState("Consent info update requested")
    }

    /**
     * @return whether the consent form is handled, i.e. shown or not required.
     */
    private suspend fun loadAndShowConsentFormIfRequired(
        activity: Activity,
    ): Boolean = suspendCancellableCoroutine { cont ->
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
            if (formError != null) {
                Logger.w("AdsConsent: Failed to show the consent form. ${formError.message}")
            }
            cont.resume(formError == null)
        }
    }
}
