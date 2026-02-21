package com.kevlina.budgetplus.core.common

import com.kevlina.budgetplus.core.common.Constants.APP_LANGUAGE_INITIALIZED_KEY
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

@ContributesIntoSet(AppScope::class)
class IosLocaleInitializer : AppStartAction {

    override fun onAppStart() {
        val userDefaults = NSUserDefaults.standardUserDefaults
        if (userDefaults.boolForKey(APP_LANGUAGE_INITIALIZED_KEY)) {
            // Language already set by the user or previously initialized
            return
        }

        val supportedLanguages = mapOf(
            "zh-Hant" to "zh-tw",
            "zh-Hans" to "zh-cn",
            "ja" to "ja",
            "en" to "en"
        )

        val preferredLanguages = NSLocale.preferredLanguages
        val bestMatch = preferredLanguages
            .asSequence()
            .filterIsInstance<String>()
            .firstNotNullOfOrNull { lang ->
                supportedLanguages.entries
                    .firstOrNull { (iosPrefix, _) -> lang.startsWith(iosPrefix) }
                    ?.value
            } ?: "en"

        userDefaults.setObject(listOf(bestMatch), "AppleLanguages")
        userDefaults.setBool(true, APP_LANGUAGE_INITIALIZED_KEY)
        userDefaults.synchronize()
    }
}