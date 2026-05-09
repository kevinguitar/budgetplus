package com.kevlina.budgetplus.feature.speak.record.impl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.record_speech_recognition_code
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.settings.api.SpeakToRecordLanguage
import com.kevlina.budgetplus.core.settings.api.SpeakToRecordSettingsViewModel
import com.kevlina.budgetplus.feature.speak.record.RecordActor
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecord
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecordStatus
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.getString
import java.util.*

@ContributesBinding(AppScope::class)
internal class SpeakToRecordImpl(
    private val context: Context,
    private val settings: SpeakToRecordSettingsViewModel,
    private val speakResultParser: SpeakResultParser,
    private val tracker: Tracker,
) : SpeakToRecord {

    override suspend fun startRecording(): RecordActor {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Logger.e(SpeakToRecordException("Feature is not supported")) { "Feature is not supported" }
            return RecordActor(
                statusFlow = flowOf(SpeakToRecordStatus.DeviceNotSupported),
                stopRecording = {}
            )
        }

        val statusFlow = MutableSharedFlow<SpeakToRecordStatus>(
            // Not acceptable to lose any events
            extraBufferCapacity = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(object : SimpleRecognitionListener() {
            override fun onReadyForSpeech(bundle: Bundle) {
                Logger.d { "SpeechRecognizer: Ready for speech" }
                statusFlow.tryEmit(SpeakToRecordStatus.ReadyToSpeak)
            }

            override fun onResults(results: Bundle) {
                recognizer.destroy()

                val data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Logger.d { "SpeechRecognizer: Results received $data" }
                // First element is the most likely candidate.
                statusFlow.tryEmit(speakResultParser.parse(data?.firstOrNull()))
            }

            override fun onError(code: Int) {
                recognizer.destroy()

                Logger.e(SpeakToRecordException("Error $code")) { "SpeechRecognizer Error $code" }
                val status = SpeakToRecordStatus.fromErrorCode(code)
                statusFlow.tryEmit(status)

                if (status is SpeakToRecordStatus.Error) {
                    tracker.logEvent(
                        event = "speak_to_record_error",
                        params = mapOf("code" to code)
                    )
                }
            }
        })

        val speakToRecordLocale = resolveSpeakToRecordLocale()
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                speakToRecordLocale
            )

        Logger.d { "SpeechRecognizer: Start listening, locale=${speakToRecordLocale}" }
        tracker.logEvent("speak_to_record_start")
        recognizer.startListening(recognizerIntent)

        return RecordActor(
            statusFlow = statusFlow,
            stopRecording = {
                statusFlow.tryEmit(SpeakToRecordStatus.Recognizing)
                recognizer.stopListening()
            }
        )
    }

    private suspend fun resolveSpeakToRecordLocale(): Locale {
        return when (settings.speakToRecordLanguage.first()) {
            SpeakToRecordLanguage.SystemLanguage -> Locale.getDefault()
            SpeakToRecordLanguage.AppLanguage -> {
                Locale.forLanguageTag(getString(Res.string.record_speech_recognition_code))
            }
        }
    }
}

private fun SpeakToRecordStatus.Companion.fromErrorCode(code: Int): SpeakToRecordStatus {
    val message = when (code) {
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout error"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_CLIENT -> "Client error"
        SpeechRecognizer.ERROR_NO_MATCH -> return SpeakToRecordStatus.NoResult
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "The requested language is not supported"
        else -> "Unknown error"
    }
    return SpeakToRecordStatus.Error(message)
}
