package com.kevlina.budgetplus.core.ads.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.ads.AdsConsentManager
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeAdsConsentManager(
    canRequestAds: Boolean = true,
    isPrivacyOptionsRequired: Boolean = false,
) : AdsConsentManager {

    override val canRequestAds = MutableStateFlow(canRequestAds)

    override val isPrivacyOptionsRequired = MutableStateFlow(isPrivacyOptionsRequired)

    var privacyOptionsFormShownCount = 0

    override suspend fun updateConsentInfo() = Unit

    override suspend fun gatherConsent(): Boolean = canRequestAds.value

    override fun showPrivacyOptionsForm() {
        privacyOptionsFormShownCount++
    }
}
