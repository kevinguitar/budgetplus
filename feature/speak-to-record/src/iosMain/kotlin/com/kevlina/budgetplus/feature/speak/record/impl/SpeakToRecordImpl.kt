package com.kevlina.budgetplus.feature.speak.record.impl

import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.appLocale
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
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer

@ContributesBinding(AppScope::class)
internal class SpeakToRecordImpl(
    private val settings: SpeakToRecordSettingsViewModel,
    private val speakResultParser: SpeakResultParser,
    private val tracker: Tracker,
) : SpeakToRecord {

    override suspend fun startRecording(): RecordActor {
        val locale = when (settings.speakToRecordLanguage.first()) {
            SpeakToRecordLanguage.AppLanguage -> NSLocale.appLocale
            SpeakToRecordLanguage.SystemLanguage -> NSLocale.currentLocale
        }
        val speechRecognizer = SFSpeechRecognizer(locale)
        if (!speechRecognizer.isAvailable()) {
            Logger.e(SpeakToRecordException("Feature is not supported")) { "Feature is not supported" }
            return RecordActor(
                statusFlow = flowOf(SpeakToRecordStatus.DeviceNotSupported),
                stopRecording = {}
            )
        }

        val statusFlow = MutableSharedFlow<SpeakToRecordStatus>(
            replay = 1,
            // Not acceptable to lose any events
            extraBufferCapacity = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val audioEngine = AVAudioEngine()
        val recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        recognitionRequest.shouldReportPartialResults = false

        var isStopped = false
        fun stopAudioEngine() {
            if (isStopped) return
            isStopped = true
            audioEngine.stop()
            audioEngine.inputNode.removeTapOnBus(0u)
        }

        speechRecognizer.recognitionTaskWithRequest(recognitionRequest) { result, error ->
            if (error != null) {
                stopAudioEngine()

                val errorMessage = error.localizedDescription
                Logger.e(SpeakToRecordException(errorMessage)) { "SpeechRecognizer Error: $errorMessage" }

                when (error.code) {
                    // Error code 1 = "Retry" which is like no match
                    1L, 203L -> statusFlow.tryEmit(SpeakToRecordStatus.NoResult)
                    // Error code 216 = request was cancelled (user stopped), ignore
                    216L -> Unit
                    else -> {
                        statusFlow.tryEmit(SpeakToRecordStatus.Error(errorMessage))
                        tracker.logEvent(
                            event = "speak_to_record_error",
                            params = mapOf("code" to error.code)
                        )
                    }
                }
                return@recognitionTaskWithRequest
            }

            if (result != null && result.isFinal()) {
                stopAudioEngine()

                val text = result.bestTranscription.formattedString
                Logger.d { "SpeechRecognizer: Results received $text" }
                statusFlow.tryEmit(speakResultParser.parse(text))
            }
        }

        // Configure audio session
        val audioSession = AVAudioSession.sharedInstance()
        audioSession.setCategory(AVAudioSessionCategoryRecord, AVAudioSessionModeMeasurement, options = 0u, null)
        audioSession.setActive(true, null)

        // Install tap on the audio input node
        val inputNode = audioEngine.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0u)
        inputNode.installTapOnBus(0u, bufferSize = 1024u, format = recordingFormat) { buffer, _ ->
            if (buffer != null) {
                recognitionRequest.appendAudioPCMBuffer(buffer)
            }
        }

        audioEngine.prepare()
        audioEngine.startAndReturnError(null)
        statusFlow.tryEmit(SpeakToRecordStatus.ReadyToSpeak)

        Logger.d { "SpeechRecognizer: Start listening, locale=${locale.localeIdentifier}" }
        tracker.logEvent("speak_to_record_start")

        return RecordActor(
            statusFlow = statusFlow,
            stopRecording = {
                statusFlow.tryEmit(SpeakToRecordStatus.Recognizing)
                recognitionRequest.endAudio()
                stopAudioEngine()
            }
        )
    }
}

