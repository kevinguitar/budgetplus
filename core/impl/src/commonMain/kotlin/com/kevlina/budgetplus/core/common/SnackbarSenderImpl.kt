package com.kevlina.budgetplus.core.common

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.fallback_error_message
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SnackbarSenderImpl(
    @AppCoroutineScope private val appScope: CoroutineScope,
) : SnackbarSender {

    override val snackbarEvent: EventFlow<SnackbarData>
        field = MutableEventFlow<SnackbarData>()

    override suspend fun send(
        message: StringResource,
        actionLabel: StringResource?,
        duration: SnackbarDuration,
        action: () -> Unit,
    ) = send(
        message = getString(message),
        actionLabel = actionLabel,
        duration = duration,
        action = action
    )

    override suspend fun send(
        message: String,
        actionLabel: StringResource?,
        duration: SnackbarDuration,
        action: () -> Unit,
    ) {
        Logger.d { "SnackbarSender: Show snackbar $message" }
        snackbarEvent.sendEvent(SnackbarData(
            message = message,
            actionLabel = actionLabel?.let { getString(it) },
            duration = duration,
            action = action
        ))
    }

    override fun sendError(e: Throwable) {
        val error = e.message
        Logger.e(e) { "SnackbarSender: sendError $error" }
        when {
            // Do not toast the cancellation error
            e is CancellationException -> Unit
            // Insignificant error, just launch from appScope
            error != null -> appScope.launch { send(error) }
            else -> appScope.launch { send(Res.string.fallback_error_message) }
        }
    }
}