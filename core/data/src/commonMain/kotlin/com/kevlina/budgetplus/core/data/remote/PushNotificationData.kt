package com.kevlina.budgetplus.core.data.remote

import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class PushNotificationData(
    val internal: Boolean? = null,
    val audienceTarget: String? = null,
    val titleTw: CharSequence? = null,
    val descTw: CharSequence? = null,
    val titleCn: CharSequence? = null,
    val descCn: CharSequence? = null,
    val titleJa: CharSequence? = null,
    val descJa: CharSequence? = null,
    val titleEn: CharSequence? = null,
    val descEn: CharSequence? = null,
    val deeplink: CharSequence? = null,
    val sentOn: Long? = null,
    val createdOn: Long? = Clock.System.now().toEpochMilliseconds(),
)
