package com.kevlina.budgetplus.feature.speak.record.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_mic
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.InfiniteCircularProgress
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.rippleIndication
import com.kevlina.budgetplus.core.ui.thenIf
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.vectorResource

@Composable
fun ColumnScope.SpeakToRecordButton(
    state: SpeakToRecordButtonState,
    isAdaptive: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val vibrateOnInput by state.vibrateOnPress.collectAsStateWithLifecycle()
    val showLoader by state.showLoader.collectAsStateWithLifecycle()
    val showRecordingDialog by state.showRecordingDialog.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val factory = rememberPermissionsControllerFactory()
    val controller = remember(factory) { factory.createPermissionsController() }
    BindEffect(controller)

    val hapticFeedback = LocalHapticFeedback.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(LocalAppColors.current.dark)
            .weight(1F)
            .thenIf(isAdaptive) { Modifier.fillMaxWidth() }
            .thenIf(!isAdaptive) { Modifier.aspectRatio(1F) }
            .rippleIndication(interactionSource)
            .onPlaced {
                state.highlightRecordButton(
                    BubbleDest.SpeakToRecord(
                        size = it.size,
                        offset = it::positionInRoot
                    )
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        scope.launch {
                            try {
                                if (controller.isPermissionGranted(Permission.RECORD_AUDIO)) {
                                    // If it's currently loading, do not trigger the tap again
                                    if (showLoader) return@launch

                                    if (vibrateOnInput) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                    }
                                    state.onTap()
                                    val press = PressInteraction.Press(offset)
                                    interactionSource.emit(press)

                                    tryAwaitRelease()

                                    state.onReleased()
                                    interactionSource.emit(PressInteraction.Release(press))
                                } else {
                                    controller.providePermission(Permission.RECORD_AUDIO)
                                }
                            } catch (_: DeniedException) {
                                // Do nothing
                            } catch (_: DeniedAlwaysException) {
                                state.showRecordPermissionHint()
                            }
                        }
                    }
                )
            }
    ) {
        if (showLoader) {
            InfiniteCircularProgress(
                color = LocalAppColors.current.light,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_mic),
                tint = LocalAppColors.current.light
            )
        }
    }

    if (showRecordingDialog) {
        SpeakToRecordDialog()
    }
}

@Stable
data class SpeakToRecordButtonState(
    val onTap: () -> Unit,
    val onReleased: () -> Unit,
    val vibrateOnPress: StateFlow<Boolean>,
    val showLoader: StateFlow<Boolean>,
    val showRecordingDialog: StateFlow<Boolean>,
    val highlightRecordButton: (BubbleDest) -> Unit,
    val showRecordPermissionHint: () -> Unit,
) {
    companion object {
        val preview = SpeakToRecordButtonState(
            onTap = {},
            onReleased = {},
            vibrateOnPress = MutableStateFlow(true),
            showLoader = MutableStateFlow(false),
            showRecordingDialog = MutableStateFlow(false),
            highlightRecordButton = {},
            showRecordPermissionHint = {},
        )
    }
}

@Preview
@Composable
private fun SpeakToRecordButton_Preview() = AppTheme {
    Column(
        modifier = Modifier
            .width(100.dp)
            .height(220.dp)
    ) {
        SpeakToRecordButton(
            state = SpeakToRecordButtonState.preview,
            isAdaptive = false
        )
        SpeakToRecordButton(
            state = SpeakToRecordButtonState.preview.copy(
                showLoader = MutableStateFlow(true)
            ),
            isAdaptive = false
        )
    }
}