package com.kevlina.budgetplus.core.common.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.common.ShareHelper
import org.jetbrains.compose.resources.StringResource

@RestrictTo(RestrictTo.Scope.TESTS)
object FakeShareHelper : ShareHelper {
    override suspend fun share(title: StringResource, text: String) = Unit
}