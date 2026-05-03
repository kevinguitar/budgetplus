package com.kevlina.budgetplus.feature.settings

import com.kevlina.budgetplus.core.common.fixtures.FakeShareHelper
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.core.data.fixtures.FakeAuthManager
import com.kevlina.budgetplus.core.unit.test.MainDispatcherRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsNavigationViewModelTest {

    @get:Rule
    val rule = MainDispatcherRule()

    @Test
    fun `openLanguageSettings logs event`() = runTest {
        val tracker = FakeTracker()
        val model = createModel(tracker = tracker)
        model.openLanguageSettings {}
        assertEquals("settings_language_click", tracker.lastEventName)
    }

    @Test
    fun `openLanguageSettings delegates to navigation`() = runTest {
        val navigation = mockk<SettingsNavigation>(relaxed = true)
        val model = createModel(navigation = navigation)
        val callback: (String) -> Unit = {}
        model.openLanguageSettings(callback)
        verify { navigation.openLanguageSettings(callback) }
    }

    @Test
    fun `rateUs logs event and opens url`() = runTest {
        val tracker = FakeTracker()
        val navigation = mockk<SettingsNavigation>(relaxed = true)
        val storeUrl = "https://play.google.com/store/apps/details?id=test"
        val model = createModel(tracker = tracker, navigation = navigation, storeReviewUrl = storeUrl)
        model.rateUs()
        assertEquals("settings_rate_us_click", tracker.lastEventName)
        verify { navigation.visitUrl(storeUrl) }
    }

    @Test
    fun `followOnInstagram logs event and opens url`() = runTest {
        val tracker = FakeTracker()
        val navigation = mockk<SettingsNavigation>(relaxed = true)
        val instagramUrl = "https://instagram.com/test"
        val model = createModel(tracker = tracker, navigation = navigation, instagramUrl = instagramUrl)
        model.followOnInstagram()
        assertEquals("settings_follow_instagram_click", tracker.lastEventName)
        verify { navigation.visitUrl(instagramUrl) }
    }

    @Test
    fun `viewPrivacyPolicy logs event and opens url`() = runTest {
        val tracker = FakeTracker()
        val navigation = mockk<SettingsNavigation>(relaxed = true)
        val privacyUrl = "https://example.com/privacy"
        val model = createModel(tracker = tracker, navigation = navigation, privacyPolicyUrl = privacyUrl)
        model.viewPrivacyPolicy()
        assertEquals("settings_privacy_policy_click", tracker.lastEventName)
        verify { navigation.visitUrl(privacyUrl) }
    }

    private fun createModel(
        tracker: FakeTracker = FakeTracker(),
        navigation: SettingsNavigation = mockk(relaxed = true),
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
