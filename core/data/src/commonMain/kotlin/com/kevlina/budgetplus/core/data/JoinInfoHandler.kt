package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.data.remote.JoinInfo

interface JoinInfoHandler {
    suspend fun generateJoinId(bookId: String): String
    suspend fun resolveJoinId(joinId: String): JoinInfo?
}
