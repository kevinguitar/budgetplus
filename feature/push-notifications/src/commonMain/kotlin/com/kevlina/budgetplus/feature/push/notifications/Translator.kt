package com.kevlina.budgetplus.feature.push.notifications

interface Translator {
    suspend fun translate(
        text: String,
        sourceLanCode: String?,
        targetLanCode: String,
    ): String
}
