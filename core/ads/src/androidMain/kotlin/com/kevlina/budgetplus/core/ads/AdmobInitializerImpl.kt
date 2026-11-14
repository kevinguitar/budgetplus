package com.kevlina.budgetplus.core.ads

import com.google.android.gms.ads.MobileAds
import com.kevlina.budgetplus.core.common.ActivityProvider
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<AppStartAction>())
@ContributesBinding(AppScope::class, binding = binding<AdmobInitializer>())
internal class AdmobInitializerImpl(
    private val activityProvider: ActivityProvider,
    private val adsConsentManager: AdsConsentManager,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : AppStartAction, AdmobInitializer {

    override val adsAvailability: StateFlow<AdsAvailability>
        field = MutableStateFlow(AdsAvailability.Pending)

    override fun onAppStart() {
        // Start the SDK as soon as ads can be requested. For the returning users, it's right after
        // the consent info update is sent, with the consent from the previous session. For the
        // new users who don't require consent, it's when the update response arrives.
        // Otherwise, it waits for the consent to be gathered in requestConsent().
        appScope.launch {
            adsConsentManager.canRequestAds.first { it }
            initAdmob()
        }
        // Refresh the consent info on every app launch, without showing the form.
        appScope.launch {
            adsConsentManager.updateConsentInfo()
        }
    }

    override fun requestConsent() {
        appScope.launch {
            if (adsConsentManager.gatherConsent()) {
                initAdmob()
            } else if (adsAvailability.value == AdsAvailability.Pending) {
                adsAvailability.value = AdsAvailability.Unavailable
            }
        }
    }

    // Only accessed on the main thread through appScope.
    private var isAdmobInitialized = false

    private suspend fun initAdmob() {
        if (isAdmobInitialized) return
        isAdmobInitialized = true

        val activity = activityProvider.activityFlow
            .filterNotNull()
            .first()

        Logger.d("AdMob: Starting the Mobile Ads SDK")
        MobileAds.initialize(activity)
        adsAvailability.value = AdsAvailability.Available
    }
}
