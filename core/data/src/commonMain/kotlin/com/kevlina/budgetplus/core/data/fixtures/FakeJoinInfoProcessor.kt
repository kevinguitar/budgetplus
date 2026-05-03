package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.JoinInfoHandler
import com.kevlina.budgetplus.core.data.remote.JoinInfo

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeJoinInfoProcessor : JoinInfoHandler {
    override suspend fun generateJoinId(bookId: String): String = "fake_join_id"
    override suspend fun resolveJoinId(joinId: String): JoinInfo? = null
}
