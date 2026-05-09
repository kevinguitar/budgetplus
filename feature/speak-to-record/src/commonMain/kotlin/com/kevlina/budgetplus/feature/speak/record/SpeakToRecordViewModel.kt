package com.kevlina.budgetplus.feature.speak.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.permission_hint
import budgetplus.core.common.generated.resources.record_speech_recognition_no_result
import budgetplus.core.common.generated.resources.record_speech_recognition_not_supported
import com.kevlina.budgetplus.core.common.EventFlow
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.sendEvent
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.bubble.BubbleRepo
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Inject
class SpeakToRecordViewModel(
    private val speakToRecord: SpeakToRecord,
    private val snackbarSender: SnackbarSender,
    private val bubbleRepo: BubbleRepo,
) : ViewModel() {

    val speakResultFlow: EventFlow<SpeakToRecordStatus.Success>
        field = MutableEventFlow<SpeakToRecordStatus.Success>()

    private val recordActorFlow = MutableStateFlow<RecordActor?>(null)

    private val recordStatusFlow = recordActorFlow
        .flatMapLatest { it?.statusFlow ?: emptyFlow() }

    val showLoader: StateFlow<Boolean> = recordStatusFlow
        .flatMapLatest {
            when (it) {
                SpeakToRecordStatus.ReadyToSpeak -> flowOf(true)
                SpeakToRecordStatus.Recognizing -> flow {
                    emit(true)
                    // Give recognition a small timeout, if for some reason the speech recognition stuck,
                    // user will be able to retry without seeing an infinite loader.
                    delay(1.seconds)
                    emit(false)
                }

                SpeakToRecordStatus.DeviceNotSupported,
                is SpeakToRecordStatus.Error,
                SpeakToRecordStatus.NoResult,
                is SpeakToRecordStatus.Success,
                    -> flowOf(false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    val showRecordingDialog: StateFlow<Boolean> = recordStatusFlow
        .map { it is SpeakToRecordStatus.ReadyToSpeak }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    init {
        recordStatusFlow
            .onEach(::handleStatus)
            .launchIn(viewModelScope)
    }

    fun onButtonTap() {
        recordActorFlow.value = speakToRecord.startRecording()
    }

    fun onButtonReleased() {
        recordActorFlow.value?.stopRecording?.invoke()
    }

    fun highlightRecordButton(dest: BubbleDest) {
        viewModelScope.launch { bubbleRepo.addBubbleToQueue(dest) }
    }

    fun showRecordPermissionHint() {
        viewModelScope.launch { snackbarSender.send(Res.string.permission_hint) }
    }

    private suspend fun handleStatus(status: SpeakToRecordStatus) {
        when (status) {
            SpeakToRecordStatus.DeviceNotSupported -> {
                snackbarSender.send(Res.string.record_speech_recognition_not_supported)
            }

            SpeakToRecordStatus.ReadyToSpeak, SpeakToRecordStatus.Recognizing -> Unit

            is SpeakToRecordStatus.Error -> snackbarSender.send(status.message)

            SpeakToRecordStatus.NoResult -> {
                snackbarSender.send(Res.string.record_speech_recognition_no_result)
            }

            is SpeakToRecordStatus.Success -> {
                speakResultFlow.sendEvent(status)
            }
        }
    }
}