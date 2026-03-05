package com.kevlina.budgetplus.feature.push.notifications

import kotlinx.serialization.Serializable

@Serializable
internal data class PushNotificationCache(
    val titleTw: String = "\uD83C\uDF1E炎夏八月，一起編織理財夢！",
    val descriptionTw: String = "記得目標，存款不停歇！記帳確實，未來更悠遊！將花費化為理財力！GO~",
    val titleJa: String = "新しい月ですよ！",
    val descriptionJa: String = "月初めに支出の追跡を始めましょう \uD83D\uDE4C",
    val titleEn: String = "It's a new month!",
    val descriptionEn: String = "Track your expenses starting from the beginning of the month \uD83D\uDE4C",
    val deeplink: String = "",
)