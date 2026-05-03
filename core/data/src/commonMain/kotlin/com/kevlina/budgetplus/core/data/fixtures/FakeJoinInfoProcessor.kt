package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.data.JoinInfoProcessor
import com.kevlina.budgetplus.core.data.remote.JoinInfo

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeJoinInfoProcessor : JoinInfoProcessor {
    override suspend fun generateJoinId(bookId: String): String = "fake_join_id"
    override suspend fun resolveJoinId(joinId: String): JoinInfo? = null
}
