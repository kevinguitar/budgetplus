package com.kevlina.budgetplus.feature.speak.record

import kotlinx.coroutines.flow.Flow

interface SpeakToRecord {

    /**
     * Start the speech recognition service, interact with [RecordActor] to listen
     * to all possible statues, and stop recording.
     */
    suspend fun startRecording(): RecordActor

}

class RecordActor(
    val statusFlow: Flow<SpeakToRecordStatus>,
    val stopRecording: () -> Unit,
)
