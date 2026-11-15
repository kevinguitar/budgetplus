package com.kevlina.budgetplus.core.ads

import kotlinx.coroutines.flow.StateFlow

interface AdmobInitializer {

    /**
     * Whether ads can be requested. Ads must not be requested before the user's consent is
     * resolved, otherwise the ad requests could be made without a valid GDPR consent.
     */
    val adsAvailability: StateFlow<AdsAvailability>

    /**
     * Gathers the user's consent for ads (the GDPR consent, plus the App Tracking Transparency
     * permission on iOS), then starts the Mobile Ads SDK if ads can be requested.
     * Should be called when the ads are about to be shown.
     */
    fun requestConsent()
}

enum class AdsAvailability {
    /** The consent is still being resolved. */
    Pending,

    /** The consent is resolved and the Mobile Ads SDK is started. */
    Available,

    /** The user hasn't given the consent that ads require. */
    Unavailable,
}
