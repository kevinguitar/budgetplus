package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.FcmTokenProvider

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeFcmTokenProvider(
    var token: String? = null,
) : FcmTokenProvider {

    override suspend fun getToken(): String? = token
}
