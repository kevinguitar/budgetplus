package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.AppLanguageProvider

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeAppLanguageProvider(
    var language: String = "en",
) : AppLanguageProvider {

    override suspend fun getLanguage(): String = language
}
