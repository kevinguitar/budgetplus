package com.kevlina.budgetplus.core.settings.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.preferences.core.stringPreferencesKey
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_app_settings_alt
import budgetplus.core.common.generated.resources.ic_apps
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.data.local.Preference
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.vectorResource

@Serializable
enum class SpeakToRecordLanguage {
    SystemLanguage, AppLanguage
}

val SpeakToRecordLanguage.icon: ImageVector
    @Composable
    get() = when (this) {
        SpeakToRecordLanguage.SystemLanguage -> vectorResource(Res.drawable.ic_app_settings_alt)
        SpeakToRecordLanguage.AppLanguage -> vectorResource(Res.drawable.ic_apps)
    }

@Inject
class SpeakToRecordSettingsViewModel(
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val preference: Preference,
    private val tracker: Tracker,
) {
    private val speakToRecordLanguageKey = stringPreferencesKey("speakToRecordLanguage")

    val speakToRecordLanguage: Flow<SpeakToRecordLanguage> = preference.of(
        key = speakToRecordLanguageKey,
        serializer = SpeakToRecordLanguage.serializer(),
    ).map { it ?: SpeakToRecordLanguage.SystemLanguage }

    fun setSpeakToRecordLanguage(mode: SpeakToRecordLanguage) {
        appScope.launch {
            preference.update(speakToRecordLanguageKey, SpeakToRecordLanguage.serializer(), mode)
        }
        tracker.logEvent(
            event = "speak_to_record_language_changed",
            params = mapOf("language" to when (mode) {
                SpeakToRecordLanguage.SystemLanguage -> "system_language"
                SpeakToRecordLanguage.AppLanguage -> "app_language"
            })
        )
    }
}