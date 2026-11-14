package com.kevlina.budgetplus.core.ads.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.ads.AdmobInitializer
import com.kevlina.budgetplus.core.ads.AdsAvailability
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeAdmobInitializer(
    adsAvailability: AdsAvailability = AdsAvailability.Available,
) : AdmobInitializer {

    override val adsAvailability = MutableStateFlow(adsAvailability)

    var consentRequestCount = 0

    override fun requestConsent() {
        consentRequestCount++
    }
}
