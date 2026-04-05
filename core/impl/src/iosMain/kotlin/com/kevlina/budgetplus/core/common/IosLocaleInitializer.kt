package com.kevlina.budgetplus.core.common

import com.kevlina.budgetplus.core.common.Constants.APP_LANGUAGE_INITIALIZED_KEY
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

@ContributesIntoSet(AppScope::class)
internal class IosLocaleInitializer : AppStartAction {

    override fun onAppStart() {
        val userDefaults = NSUserDefaults.standardUserDefaults
        if (userDefaults.boolForKey(APP_LANGUAGE_INITIALIZED_KEY)) {
            // Language already set by the user or previously initialized
            return
        }

        val preferredLanguages = NSLocale.preferredLanguages
            .asSequence()
            .filterIsInstance<String>()

        val bestMatch = resolveSupportedAppLanguage(preferredLanguages.toList()) ?: return

        userDefaults.setObject(listOf(bestMatch), "AppleLanguages")
        userDefaults.setBool(true, APP_LANGUAGE_INITIALIZED_KEY)
        userDefaults.synchronize()
    }
}