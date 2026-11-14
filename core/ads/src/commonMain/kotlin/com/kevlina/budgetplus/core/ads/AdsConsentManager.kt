package com.kevlina.budgetplus.core.ads

import kotlinx.coroutines.flow.StateFlow

/**
 * Gathers the user's consent for ads through Google's User Messaging Platform (UMP), as required
 * by the GDPR for users in the EEA, the UK and Switzerland.
 *
 * The consent messages themselves (GDPR, IDFA explainer, etc.) are configured in the
 * "Privacy & messaging" section of the AdMob console.
 */
interface AdsConsentManager {

    /**
     * Whether ads can be requested with the consent gathered so far. It's always false until the
     * consent info is requested in the current session, through [updateConsentInfo] or
     * [gatherConsent]. Right after the request is sent, it reflects the consent from the previous
     * session, then it's updated with the response.
     */
    val canRequestAds: StateFlow<Boolean>

    /**
     * Whether the user must be offered an entry point to change their privacy choices, which is
     * the case for the users in the regions where the consent is required.
     */
    val isPrivacyOptionsRequired: StateFlow<Boolean>

    /**
     * Requests the latest consent information. Doesn't show any form, can be called on every
     * app launch to refresh [canRequestAds] and [isPrivacyOptionsRequired].
     */
    suspend fun updateConsentInfo()

    /**
     * Requests the latest consent information, then shows the consent form if the user hasn't
     * given their consent yet. Only does the work once per session.
     *
     * @return whether ads can be requested once the consent is gathered.
     */
    suspend fun gatherConsent(): Boolean

    /**
     * Shows the privacy options form, so the user can change their choices at any time.
     */
    fun showPrivacyOptionsForm()
}
