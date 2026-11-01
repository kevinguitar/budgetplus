package com.kevlina.budgetplus.core.common

import org.jetbrains.compose.resources.StringResource

/**
 *  Deliver the snackbar event to let ui show it.
 */
interface SnackbarSender {

    val snackbarEvent: EventFlow<SnackbarData>

     suspend fun send(
        message: StringResource,
        actionLabel: StringResource? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        action: () -> Unit = {},
    )

    /**
     * Send the formatted string resource, e.g. `Switched to %1$s`. A [StringResource] in
     * [formatArgs] is resolved before formatting, e.g. the app name.
     */
    suspend fun send(
        message: StringResource,
        formatArgs: List<Any>,
        duration: SnackbarDuration = SnackbarDuration.Short,
    )

    suspend fun send(
        message: String,
        actionLabel: StringResource? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        action: () -> Unit = {},
    )

    fun sendError(e: Throwable)
}

class SnackbarData(
    val message: String,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val action: () -> Unit = {},
)

enum class SnackbarDuration {
    Short, Long, Indefinite
}