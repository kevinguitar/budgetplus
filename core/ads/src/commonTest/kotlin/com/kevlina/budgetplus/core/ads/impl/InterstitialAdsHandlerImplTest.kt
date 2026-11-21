package com.kevlina.budgetplus.core.ads.impl

import androidx.datastore.preferences.core.stringPreferencesKey
import com.kevlina.budgetplus.core.ads.AdsAvailability
import com.kevlina.budgetplus.core.ads.InterstitialAdRequest
import com.kevlina.budgetplus.core.ads.InterstitialAdsConfig
import com.kevlina.budgetplus.core.ads.InterstitialHistory
import com.kevlina.budgetplus.core.ads.InterstitialPlacement
import com.kevlina.budgetplus.core.ads.fixtures.FakeAdmobInitializer
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.data.fixtures.FakePreference
import com.kevlina.budgetplus.core.data.fixtures.FakeRemoteConfig
import com.kevlina.budgetplus.core.data.remote.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class InterstitialAdsHandlerImplTest {

    private val historyKey = stringPreferencesKey("interstitialHistory")
    private val tracker = FakeTracker()
    private val oldUser = User(
        id = "user",
        premium = false,
        createdOn = (Clock.System.now() - 30.days).toEpochMilliseconds()
    )

    // The experiment config, the defaults are the legacy behavior.
    private val historyTabConfig = mapOf<String, Any>(
        InterstitialAdsConfig.KEY_PLACEMENTS to "enter_history,leave_detail",
        InterstitialAdsConfig.KEY_MIN_INTERVAL_MINUTES to 30,
        InterstitialAdsConfig.KEY_DAILY_CAP to 2,
        InterstitialAdsConfig.KEY_NEW_USER_GRACE_DAYS to 3,
    )

    @Test
    fun `legacy defaults show the record saved and csv export ads, even to new users`() =
        runTest(UnconfinedTestDispatcher()) {
            val newUser = oldUser.copy(createdOn = Clock.System.now().toEpochMilliseconds())
            val handler = createHandler(user = newUser, remoteConfig = emptyMap())

            assertTrue(handler.isEligible.value)
            handler.showAd(InterstitialPlacement.RecordSaved)
            assertNotNull(handler.pendingRequest())
            handler.showAd(InterstitialPlacement.CsvExport)
            assertNotNull(handler.pendingRequest())
            handler.showAd(InterstitialPlacement.EnterHistory)
            assertNull(handler.pendingRequest())
        }

    @Test
    fun `eligible for a free user past the grace period`() = runTest(UnconfinedTestDispatcher()) {
        assertTrue(createHandler().isEligible.value)
    }

    @Test
    fun `not eligible for premium users`() = runTest(UnconfinedTestDispatcher()) {
        val handler = createHandler(user = oldUser.copy(premium = true))
        assertFalse(handler.isEligible.value)
    }

    @Test
    fun `not eligible during the new user grace period`() = runTest(UnconfinedTestDispatcher()) {
        val newUser = oldUser.copy(createdOn = (Clock.System.now() - 1.days).toEpochMilliseconds())
        assertFalse(createHandler(user = newUser).isEligible.value)
    }

    @Test
    fun `not eligible before the ads consent is resolved`() = runTest(UnconfinedTestDispatcher()) {
        val handler = createHandler(adsAvailability = AdsAvailability.Pending)
        assertFalse(handler.isEligible.value)
    }

    @Test
    fun `not eligible when all placements are disabled`() = runTest(UnconfinedTestDispatcher()) {
        val handler = createHandler(remoteConfig = mapOf(InterstitialAdsConfig.KEY_PLACEMENTS to ""))
        assertFalse(handler.isEligible.value)
    }

    @Test
    fun `request an ad for an enabled placement`() = runTest(UnconfinedTestDispatcher()) {
        val handler = createHandler()

        handler.showAd(InterstitialPlacement.EnterHistory)

        assertNotNull(handler.pendingRequest())
    }

    @Test
    fun `don't request an ad for a disabled placement`() = runTest(UnconfinedTestDispatcher()) {
        val handler = createHandler()

        handler.showAd(InterstitialPlacement.RecordSaved)

        assertNull(handler.pendingRequest())
    }

    @Test
    fun `don't request an ad within the min interval`() = runTest(UnconfinedTestDispatcher()) {
        val history = InterstitialHistory(
            lastShownAtMillis = (Clock.System.now() - 5.minutes).toEpochMilliseconds()
        )
        val preference = FakePreference()
        preference.update(historyKey, InterstitialHistory.serializer(), history)
        val handler = createHandler(preference = preference)

        handler.showAd(InterstitialPlacement.LeaveDetail)

        assertNull(handler.pendingRequest())
    }

    @Test
    fun `record the history and track when the ad is shown`() = runTest(UnconfinedTestDispatcher()) {
        val preference = FakePreference()
        val handler = createHandler(preference = preference)

        handler.showAd(InterstitialPlacement.EnterHistory)
        handler.pendingRequest()!!.onShown()

        val history = preference.of(historyKey, InterstitialHistory.serializer()).first()
        assertNotNull(history?.lastShownAtMillis)
        assertEquals(1, history.shownCountOnDay)
        assertEquals(
            "show_ad_full_screen" to mapOf<String, Any>("placement" to "enter_history"),
            tracker.lastEvent
        )
    }

    @Test
    fun `track when the ad isn't ready`() = runTest(UnconfinedTestDispatcher()) {
        val handler = createHandler()

        handler.showAd(InterstitialPlacement.LeaveDetail)
        handler.pendingRequest()!!.onNotReady()

        assertEquals("ad_full_screen_not_ready", tracker.lastEventName)
    }

    @Test
    fun `showAdThen completes right away for a disabled placement`() = runTest(UnconfinedTestDispatcher()) {
        val handler = createHandler()
        var completed = false

        handler.showAdThen(InterstitialPlacement.CsvExport) { completed = true }

        assertTrue(completed)
        assertNull(handler.pendingRequest())
    }

    @Test
    fun `showAdThen completes right away when the caps skip the ad`() = runTest(UnconfinedTestDispatcher()) {
        val preference = FakePreference()
        preference.update(
            historyKey,
            InterstitialHistory.serializer(),
            InterstitialHistory(lastShownAtMillis = (Clock.System.now() - 5.minutes).toEpochMilliseconds())
        )
        val handler = createHandler(preference = preference)
        var completed = false

        handler.showAdThen(InterstitialPlacement.EnterHistory) { completed = true }

        assertTrue(completed)
        assertNull(handler.pendingRequest())
    }

    @Test
    fun `showAdThen completes when the ad finishes`() = runTest(UnconfinedTestDispatcher()) {
        val handler = createHandler(remoteConfig = emptyMap())
        var completed = false

        handler.showAdThen(InterstitialPlacement.CsvExport) { completed = true }
        val request = handler.pendingRequest()!!
        assertFalse(completed)

        request.onFinished()
        assertTrue(completed)
    }

    private fun InterstitialAdsHandlerImpl.pendingRequest(): InterstitialAdRequest? {
        return showAdEvent.value.consume()
    }

    private fun TestScope.createHandler(
        user: User = oldUser,
        adsAvailability: AdsAvailability = AdsAvailability.Available,
        remoteConfig: Map<String, Any> = historyTabConfig,
        preference: FakePreference = FakePreference(),
    ) = InterstitialAdsHandlerImpl(
        authManager = FakeAuthManager(user = user, isPremium = user.premium == true),
        config = InterstitialAdsConfig(FakeRemoteConfig(remoteConfig)),
        preference = preference,
        tracker = tracker,
        appScope = backgroundScope,
        admobInitializer = FakeAdmobInitializer(adsAvailability),
    )
}
