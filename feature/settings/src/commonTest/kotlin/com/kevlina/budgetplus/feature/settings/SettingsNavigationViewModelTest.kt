package com.kevlina.budgetplus.feature.settings

import com.kevlina.budgetplus.core.common.fixtures.FakeShareHelper
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsNavigationViewModelTest {

    @Test
    fun `openLanguageSettings logs event`() = runTest {
        val tracker = FakeTracker()
        val model = createModel(tracker = tracker)
        model.openLanguageSettings {}
        assertEquals("settings_language_click", tracker.lastEventName)
    }

    @Test
    fun `openLanguageSettings delegates to navigation`() = runTest {
        val navigation = FakeSettingsNavigation()
        val model = createModel(navigation = navigation)
        model.openLanguageSettings {}
        assertEquals("openLanguageSettings", navigation.lastCall)
    }

    @Test
    fun `rateUs logs event and opens url`() = runTest {
        val tracker = FakeTracker()
        val navigation = FakeSettingsNavigation()
        val storeUrl = "https://play.google.com/store/apps/details?id=test"
        val model = createModel(tracker = tracker, navigation = navigation, storeReviewUrl = storeUrl)
        model.rateUs()
        assertEquals("settings_rate_us_click", tracker.lastEventName)
        assertEquals(storeUrl, navigation.lastVisitedUrl)
    }

    @Test
    fun `followOnInstagram logs event and opens url`() = runTest {
        val tracker = FakeTracker()
        val navigation = FakeSettingsNavigation()
        val instagramUrl = "https://instagram.com/test"
        val model = createModel(tracker = tracker, navigation = navigation, instagramUrl = instagramUrl)
        model.followOnInstagram()
        assertEquals("settings_follow_instagram_click", tracker.lastEventName)
        assertEquals(instagramUrl, navigation.lastVisitedUrl)
    }

    @Test
    fun `viewPrivacyPolicy logs event and opens url`() = runTest {
        val tracker = FakeTracker()
        val navigation = FakeSettingsNavigation()
        val privacyUrl = "https://example.com/privacy"
        val model = createModel(tracker = tracker, navigation = navigation, privacyPolicyUrl = privacyUrl)
        model.viewPrivacyPolicy()
        assertEquals("settings_privacy_policy_click", tracker.lastEventName)
        assertEquals(privacyUrl, navigation.lastVisitedUrl)
    }

    private fun createModel(
        tracker: FakeTracker = FakeTracker(),
        navigation: SettingsNavigation = FakeSettingsNavigation(),
        storeReviewUrl: String = "",
        shareAppUrl: String = "",
        instagramUrl: String = "",
        privacyPolicyUrl: String = "",
    ): SettingsNavigationViewModel {
        return SettingsNavigationViewModel(
            authManager = FakeAuthManager(),
            navigation = navigation,
            shareHelper = FakeShareHelper,
            tracker = tracker,
            storeReviewUrl = storeReviewUrl,
            shareAppUrl = shareAppUrl,
            instagramUrl = instagramUrl,
            privacyPolicyUrl = privacyPolicyUrl,
        )
    }
}

private class FakeSettingsNavigation : SettingsNavigation {

    var lastCall: String? = null
    var lastVisitedUrl: String? = null

    override fun openLanguageSettings(onLanguageChanged: (String) -> Unit) {
        lastCall = "openLanguageSettings"
    }

    override suspend fun contactUs() {
        lastCall = "contactUs"
    }

    override fun visitUrl(url: String) {
        lastCall = "visitUrl"
        lastVisitedUrl = url
    }
}
