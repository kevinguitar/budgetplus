package com.kevlina.budgetplus.core.ads

import com.kevlina.budgetplus.core.data.fixtures.FakeRemoteConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class InterstitialAdsConfigTest {

    @Test
    fun `defaults keep the legacy behavior when remote config is unavailable`() {
        val config = InterstitialAdsConfig(FakeRemoteConfig())

        assertEquals(
            setOf(InterstitialPlacement.RecordSaved, InterstitialPlacement.CsvExport),
            config.enabledPlacements.value
        )
        assertEquals(
            InterstitialFrequency(minInterval = 0.minutes, dailyCap = 0, newUserGracePeriod = 0.days),
            config.frequency
        )
    }

    @Test
    fun `read the values from remote config`() {
        val config = InterstitialAdsConfig(
            FakeRemoteConfig(
                mapOf(
                    InterstitialAdsConfig.KEY_PLACEMENTS to "enter_history,leave_detail",
                    InterstitialAdsConfig.KEY_MIN_INTERVAL_MINUTES to 30,
                    InterstitialAdsConfig.KEY_DAILY_CAP to 2,
                    InterstitialAdsConfig.KEY_NEW_USER_GRACE_DAYS to 3,
                )
            )
        )

        assertEquals(
            setOf(InterstitialPlacement.EnterHistory, InterstitialPlacement.LeaveDetail),
            config.enabledPlacements.value
        )
        assertEquals(
            InterstitialFrequency(minInterval = 30.minutes, dailyCap = 2, newUserGracePeriod = 3.days),
            config.frequency
        )
    }

    @Test
    fun `negative values are treated as zero`() {
        val config = InterstitialAdsConfig(
            FakeRemoteConfig(
                mapOf(
                    InterstitialAdsConfig.KEY_MIN_INTERVAL_MINUTES to -1,
                    InterstitialAdsConfig.KEY_DAILY_CAP to -1,
                    InterstitialAdsConfig.KEY_NEW_USER_GRACE_DAYS to -1,
                )
            )
        )

        assertEquals(
            InterstitialFrequency(minInterval = 0.minutes, dailyCap = 0, newUserGracePeriod = 0.days),
            config.frequency
        )
    }

    @Test
    fun `parse placements ignores blanks and unknown keys`() {
        assertEquals(
            setOf(InterstitialPlacement.EnterHistory, InterstitialPlacement.CsvExport),
            InterstitialAdsConfig.parsePlacements(" enter_history , unknown,,csv_export ")
        )
        assertEquals(emptySet(), InterstitialAdsConfig.parsePlacements(""))
    }
}
