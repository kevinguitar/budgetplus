package com.kevlina.budgetplus.core.data.remote

import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class PushNotificationData(
    val internal: Boolean? = null,
    val audienceTarget: String? = null,
    val titleTw: String? = null,
    val descTw: String? = null,
    val titleCn: String? = null,
    val descCn: String? = null,
    val titleJa: String? = null,
    val descJa: String? = null,
    val titleEn: String? = null,
    val descEn: String? = null,
    val deeplink: String? = null,
    val sentOn: Long? = null,
    val createdOn: Long? = Clock.System.now().toEpochMilliseconds(),
)
