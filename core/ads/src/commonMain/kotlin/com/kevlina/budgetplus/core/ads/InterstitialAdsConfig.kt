package com.kevlina.budgetplus.core.ads

import com.kevlina.budgetplus.core.common.mapState
import com.kevlina.budgetplus.core.data.RemoteConfig
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * Interstitial ads settings, controlled by Firebase Remote Config so the placements and the
 * frequency can be tuned, or A/B tested, without a release.
 *
 * The defaults keep the legacy behavior, when Remote Config was never fetched: an ad on every
 * 7th record and before the CSV export, without any frequency cap.
 *
 * | Key | Type | Default |
 * |---|---|---|
 * | `ads_interstitial_placements` | Comma separated [InterstitialPlacement.key]s, empty disables all | `record_saved,csv_export` |
 * | `ads_interstitial_min_interval_minutes` | Minimum minutes between two ads, 0 for none | `0` |
 * | `ads_interstitial_daily_cap` | Maximum ads per local calendar day, 0 for no cap | `0` |
 * | `ads_interstitial_new_user_grace_days` | No ads in the first N days since sign up, 0 for none | `0` |
 */
@Inject
class InterstitialAdsConfig(
    private val remoteConfig: RemoteConfig,
) {

    val enabledPlacements: StateFlow<Set<InterstitialPlacement>> = remoteConfig
        .observeString(KEY_PLACEMENTS, DEFAULT_PLACEMENTS)
        .mapState(::parsePlacements)

    val frequency: InterstitialFrequency
        get() = InterstitialFrequency(
            minInterval = remoteConfig.getInt(KEY_MIN_INTERVAL_MINUTES, DEFAULT_MIN_INTERVAL_MINUTES)
                .coerceAtLeast(0).minutes,
            dailyCap = remoteConfig.getInt(KEY_DAILY_CAP, DEFAULT_DAILY_CAP).coerceAtLeast(0),
            newUserGracePeriod = remoteConfig.getInt(KEY_NEW_USER_GRACE_DAYS, DEFAULT_NEW_USER_GRACE_DAYS)
                .coerceAtLeast(0).days,
        )

    companion object {
        const val KEY_PLACEMENTS = "ads_interstitial_placements"
        const val KEY_MIN_INTERVAL_MINUTES = "ads_interstitial_min_interval_minutes"
        const val KEY_DAILY_CAP = "ads_interstitial_daily_cap"
        const val KEY_NEW_USER_GRACE_DAYS = "ads_interstitial_new_user_grace_days"

        const val DEFAULT_PLACEMENTS = "record_saved,csv_export"
        const val DEFAULT_MIN_INTERVAL_MINUTES = 0
        const val DEFAULT_DAILY_CAP = 0
        const val DEFAULT_NEW_USER_GRACE_DAYS = 0

        fun parsePlacements(value: String): Set<InterstitialPlacement> = value
            .split(',')
            .mapNotNull { InterstitialPlacement.fromKey(it.trim()) }
            .toSet()
    }
}

data class InterstitialFrequency(
    val minInterval: Duration,
    val dailyCap: Int,
    val newUserGracePeriod: Duration,
)
