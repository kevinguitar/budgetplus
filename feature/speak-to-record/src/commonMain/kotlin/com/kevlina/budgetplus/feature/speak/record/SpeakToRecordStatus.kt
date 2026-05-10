package com.kevlina.budgetplus.feature.speak.record

/**
 * Represents the various states or results of a speech-to-record process.
 */
sealed interface SpeakToRecordStatus {

    data object DeviceNotSupported : SpeakToRecordStatus

    data object ReadyToSpeak : SpeakToRecordStatus

    data object Recognizing : SpeakToRecordStatus

    data object NoResult : SpeakToRecordStatus

    data class Error(val message: String) : SpeakToRecordStatus

    data class Success(
        val name: String,
        val price: Double?,
    ) : SpeakToRecordStatus

    companion object
}
