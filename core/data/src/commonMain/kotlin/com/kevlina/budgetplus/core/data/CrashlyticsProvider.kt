package com.kevlina.budgetplus.core.data

/**
 *  Wraps Firebase Crashlytics operations for testability.
 */
interface CrashlyticsProvider {

    fun setUserId(userId: String)
}
