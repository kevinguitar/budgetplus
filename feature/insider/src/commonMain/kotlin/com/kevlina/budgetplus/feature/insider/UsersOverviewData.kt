package com.kevlina.budgetplus.feature.insider

internal data class InsiderData(
    val totalPremiumUsers: Long,
    val dailyActiveUsers: Long,
    val weeklyActiveUsers: Long,
)

internal data class UsersOverviewData(
    val totalEnglishUsers: Long,
    val totalJapaneseUsers: Long,
    val totalKoreanUsers: Long,
    val totalSimplifiedChineseUsers: Long,
)
