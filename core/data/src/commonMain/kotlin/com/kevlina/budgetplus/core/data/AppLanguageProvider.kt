package com.kevlina.budgetplus.core.data

/**
 *  Provides the app language string for testability.
 */
interface AppLanguageProvider {

    suspend fun getLanguage(): String
}
