package com.kevlina.budgetplus.feature.push.notifications

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.feature.push_notifications.generated.resources.Res
import budgetplus.feature.push_notifications.generated.resources.push_notif_sent_success
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.data.AuthManager
import com.kevlina.budgetplus.core.data.PushDbMediator
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.data.remote.PushNotificationData
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Named
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlin.time.Clock

@ViewModelKey
@ContributesIntoMap(AppScope::class)
class PushNotificationsViewModel private constructor(
    private val preference: Preference,
    translator: Translator,
    @Named("default_deeplink") private val defaultDeeplink: String,
    private val pushDbMediator: PushDbMediator,
    private val authManager: AuthManager,
    private val snackbarSender: SnackbarSender,
) : ViewModel() {

    val audienceTarget = MutableStateFlow(AudienceTarget.All)

    val titleTw = TextFieldState()
    val descTw = TextFieldState()

    val sendToCn = MutableStateFlow(true)
    val titleCn = TextFieldState()
    val descCn = TextFieldState()

    val sendToEn = MutableStateFlow(true)
    val titleEn = TextFieldState()
    val descEn = TextFieldState()

    val sendToJa = MutableStateFlow(true)
    val titleJa = TextFieldState()
    val descJa = TextFieldState()

    val sendToKo = MutableStateFlow(true)
    val titleKo = TextFieldState()
    val descKo = TextFieldState()

    val deeplink = TextFieldState()

    private val cacheKey = stringPreferencesKey("pushNotificationCache")

    init {
        loadCache()

        snapshotFlow { titleTw.text }
            .debounce(INPUT_DEBOUNCE_MS)
            .mapLatest {
                val title = translator.translate(
                    text = it.toString(),
                    sourceLanCode = LAN_CODE_TW,
                    targetLanCode = LAN_CODE_CN
                )
                titleCn.setTextAndPlaceCursorAtEnd(title)
            }
            .launchIn(viewModelScope)

        snapshotFlow { descTw.text }
            .debounce(INPUT_DEBOUNCE_MS)
            .mapLatest {
                val desc = translator.translate(
                    text = it.toString(),
                    sourceLanCode = LAN_CODE_TW,
                    targetLanCode = LAN_CODE_CN
                )
                descCn.setTextAndPlaceCursorAtEnd(desc)
            }
            .launchIn(viewModelScope)
    }

    fun sendToInternalTopic() {
        recordToPushDb(isInternal = true)
    }

    fun sendToEveryone() {
        recordToPushDb(isInternal = false)
    }

    private fun recordToPushDb(isInternal: Boolean) {
        saveToCache()
        viewModelScope.launch {
            try {
                val user = authManager.userState.first()
                if (user?.internal != true) {
                    error("An external user is trying to send notification!!!")
                }
                pushDbMediator.recordPushNotification(PushNotificationData(
                    internal = isInternal,
                    audienceTarget = audienceTarget.value.name,
                    titleTw = titleTw.text.trim().toString(),
                    descTw = descTw.text.trim().toString(),
                    titleCn = titleCn.text.trim().takeIf { sendToCn.value }?.toString(),
                    descCn = descCn.text.trim().takeIf { sendToCn.value }?.toString(),
                    titleEn = titleEn.text.trim().takeIf { sendToEn.value }?.toString(),
                    descEn = descEn.text.trim().takeIf { sendToEn.value }?.toString(),
                    titleJa = titleJa.text.trim().takeIf { sendToJa.value }?.toString(),
                    descJa = descJa.text.trim().takeIf { sendToJa.value }?.toString(),
                    titleKo = titleKo.text.trim().takeIf { sendToKo.value }?.toString(),
                    descKo = descKo.text.trim().takeIf { sendToKo.value }?.toString(),
                    deeplink = if (deeplink.text.isNotBlank()) {
                        deeplink.text.trim().toString()
                    } else {
                        defaultDeeplink
                    },
                    sentOn = Clock.System.now().toEpochMilliseconds()
                ))
                snackbarSender.send(Res.string.push_notif_sent_success)
            } catch (e: Exception) {
                snackbarSender.sendError(e)
            }
        }
    }

    private fun loadCache() {
        viewModelScope.launch {
            val cache = preference.of(cacheKey, PushNotificationCache.serializer()).first()
                ?: PushNotificationCache()

            titleTw.setTextAndPlaceCursorAtEnd(cache.titleTw)
            descTw.setTextAndPlaceCursorAtEnd(cache.descriptionTw)
            titleEn.setTextAndPlaceCursorAtEnd(cache.titleEn)
            descEn.setTextAndPlaceCursorAtEnd(cache.descriptionEn)
            titleJa.setTextAndPlaceCursorAtEnd(cache.titleJa)
            descJa.setTextAndPlaceCursorAtEnd(cache.descriptionJa)
            titleKo.setTextAndPlaceCursorAtEnd(cache.titleKo)
            descKo.setTextAndPlaceCursorAtEnd(cache.descriptionKo)
            deeplink.setTextAndPlaceCursorAtEnd(cache.deeplink)
        }
    }

    private fun saveToCache() {
        viewModelScope.launch {
            val newCache = PushNotificationCache(
                titleTw = titleTw.text.toString(),
                descriptionTw = descTw.text.toString(),
                titleEn = titleEn.text.toString(),
                descriptionEn = descEn.text.toString(),
                deeplink = deeplink.text.toString(),
                titleJa = titleJa.text.toString(),
                descriptionJa = descJa.text.toString(),
                titleKo = titleKo.text.toString(),
                descriptionKo = descKo.text.toString(),
            )
            preference.update(cacheKey, PushNotificationCache.serializer(), newCache)
        }
    }

    private companion object {
        const val INPUT_DEBOUNCE_MS = 200L
        const val LAN_CODE_TW = "zh-TW"
        const val LAN_CODE_CN = "zh-CN"
    }
}