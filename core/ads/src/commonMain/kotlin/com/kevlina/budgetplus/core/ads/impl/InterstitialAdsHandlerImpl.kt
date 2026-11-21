package com.kevlina.budgetplus.core.ads.impl

import androidx.datastore.preferences.core.stringPreferencesKey
import com.kevlina.budgetplus.core.ads.AdmobInitializer
import com.kevlina.budgetplus.core.ads.AdsAvailability
import com.kevlina.budgetplus.core.ads.InterstitialAdRequest
import com.kevlina.budgetplus.core.ads.InterstitialAdsConfig
import com.kevlina.budgetplus.core.ads.InterstitialAdsHandler
import com.kevlina.budgetplus.core.ads.InterstitialFrequencyPolicy
import com.kevlina.budgetplus.core.ads.InterstitialHistory
import com.kevlina.budgetplus.core.ads.InterstitialPlacement
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.Logger
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.UiTestFlags
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.data.remote.User
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class InterstitialAdsHandlerImpl(
    private val authManager: AuthManager,
    private val config: InterstitialAdsConfig,
    private val preference: Preference,
    private val tracker: Tracker,
    @AppCoroutineScope private val appScope: CoroutineScope,
    admobInitializer: AdmobInitializer,
) : InterstitialAdsHandler {

    private val historyKey = stringPreferencesKey("interstitialHistory")

    private val now: Instant get() = Clock.System.now()
    private val timeZone: TimeZone get() = TimeZone.currentSystemDefault()

    // Ads can't be requested before the user's consent is resolved.
    override val isEligible: StateFlow<Boolean> = combine(
        authManager.userState,
        admobInitializer.adsAvailability,
        config.enabledPlacements,
    ) { user, adsAvailability, placements ->
        !UiTestFlags.enabled &&
            user != null &&
            user.premium != true &&
            adsAvailability == AdsAvailability.Available &&
            placements.isNotEmpty() &&
            !InterstitialFrequencyPolicy.isNewUser(config.frequency, user.createdOnInstant, now)
    }.stateIn(appScope, SharingStarted.Eagerly, false)

    override val showAdEvent: EventFlow<InterstitialAdRequest>
        field = MutableEventFlow()

    override fun showAd(placement: InterstitialPlacement) {
        showAdThen(placement, onComplete = {})
    }

    override fun showAdThen(placement: InterstitialPlacement, onComplete: () -> Unit) {
        if (!isEligible.value || placement !in config.enabledPlacements.value) {
            onComplete()
            return
        }

        appScope.launch {
            val history = preference.of(historyKey, InterstitialHistory.serializer()).first()
                ?: InterstitialHistory()
            val skipReason = InterstitialFrequencyPolicy.skipReason(
                frequency = config.frequency,
                history = history,
                userCreatedOn = authManager.userState.value?.createdOnInstant,
                now = now,
                timeZone = timeZone,
            )
            if (skipReason != null) {
                Logger.d("InterstitialAd: Skip ${placement.key}, reason=$skipReason")
                onComplete()
                return@launch
            }

            showAdEvent.sendEvent(
                InterstitialAdRequest(
                    onShown = { onAdShown(placement) },
                    onNotReady = {
                        tracker.logEvent("ad_full_screen_not_ready", mapOf("placement" to placement.key))
                    },
                    onFinished = onComplete,
                )
            )
        }
    }

    private fun onAdShown(placement: InterstitialPlacement) {
        tracker.logEvent("show_ad_full_screen", mapOf("placement" to placement.key))
        appScope.launch {
            preference.updateTransform(historyKey, InterstitialHistory.serializer()) { current ->
                (current ?: InterstitialHistory()).recordShown(now, timeZone)
            }
        }
    }

    private val User.createdOnInstant: Instant?
        get() = createdOn?.let(Instant::fromEpochMilliseconds)
}
