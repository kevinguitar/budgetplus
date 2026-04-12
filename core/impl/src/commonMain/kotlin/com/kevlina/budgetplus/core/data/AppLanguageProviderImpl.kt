package com.kevlina.budgetplus.core.data

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.app_language
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import org.jetbrains.compose.resources.getString

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AppLanguageProviderImpl : AppLanguageProvider {

    override suspend fun getLanguage(): String {
        return getString(Res.string.app_language)
    }
}
