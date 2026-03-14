package com.kevlina.budgetplus.core.common

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.localeWithLocaleIdentifier

val NSLocale.Companion.appLocale: NSLocale
    get() {
        val languages = NSUserDefaults.standardUserDefaults.stringArrayForKey("AppleLanguages")
        val languageCode = languages?.firstOrNull() as? String
        return if (languageCode != null) {
            NSLocale.localeWithLocaleIdentifier(languageCode)
        } else {
            NSLocale.currentLocale
        }
    }
