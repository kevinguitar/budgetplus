package com.kevlina.budgetplus.core.common.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.common.MutableEventFlow
import com.kevlina.budgetplus.core.common.SnackbarData
import com.kevlina.budgetplus.core.common.SnackbarDuration
import com.kevlina.budgetplus.core.common.SnackbarSender
import org.jetbrains.compose.resources.StringResource

@RestrictTo(RestrictTo.Scope.TESTS)
object FakeSnackbarSender : SnackbarSender {

    var lastSentMessageRes: StringResource? = null
    var lastSentMessage: String? = null
    var lastSentError: Throwable? = null

    override val snackbarEvent = MutableEventFlow<SnackbarData>()

    override suspend fun send(
        message: StringResource,
        actionLabel: StringResource?,
        duration: SnackbarDuration,
        action: () -> Unit,
    ) {
        lastSentMessageRes = message
    }

    override suspend fun send(
        message: String,
        actionLabel: StringResource?,
        duration: SnackbarDuration,
        action: () -> Unit,
    ) {
        lastSentMessage = message
    }

    override fun sendError(e: Throwable) {
        lastSentError = e
    }
}