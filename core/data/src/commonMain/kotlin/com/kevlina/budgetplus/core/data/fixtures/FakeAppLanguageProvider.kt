package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.VisibleForTesting
import com.kevlina.budgetplus.core.data.AppLanguageProvider

@VisibleForTesting
class FakeAppLanguageProvider(
    var language: String = "en",
) : AppLanguageProvider {

    override suspend fun getLanguage(): String = language
}
