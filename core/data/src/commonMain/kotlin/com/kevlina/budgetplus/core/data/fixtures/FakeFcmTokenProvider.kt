package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.VisibleForTesting
import com.kevlina.budgetplus.core.data.FcmTokenProvider

@VisibleForTesting
class FakeFcmTokenProvider(
    var token: String? = null,
) : FcmTokenProvider {

    override suspend fun getToken(): String? = token
}
