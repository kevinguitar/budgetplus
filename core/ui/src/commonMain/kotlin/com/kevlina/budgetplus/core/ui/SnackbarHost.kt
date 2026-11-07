package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.common.SnackbarData
import com.kevlina.budgetplus.core.common.SnackbarDuration
import com.kevlina.budgetplus.core.common.UiTestFlags
import com.kevlina.budgetplus.core.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.material3.SnackbarDuration as MaterialSnackbarDuration
import androidx.compose.material3.SnackbarHost as MaterialSnackbarHost

/**
 *  @param bottomSpacing the distance between the snackbar and the bottom of the host, null to keep
 *  the default Material spacing.
 */
@Composable
fun SnackbarHost(
    snackbarData: SnackbarData?,
    bottomSpacing: Dp? = null,
) {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = snackbarData) {
        if (snackbarData != null) {
            // Dismiss the previous snackbar if it exists, to avoid a long queue
            hostState.currentSnackbarData?.dismiss()
            scope.launch {
                val result = hostState.showSnackbar(
                    message = snackbarData.message,
                    actionLabel = snackbarData.actionLabel,
                    // We support swipe to dismiss, don't want to pollute ui
                    withDismissAction = false,
                    duration = if (UiTestFlags.enabled) {
                        // In UI-test mode keep snackbars on screen so Maestro can assert them reliably.
                        MaterialSnackbarDuration.Indefinite
                    } else {
                        when (snackbarData.duration) {
                            SnackbarDuration.Short -> MaterialSnackbarDuration.Short
                            SnackbarDuration.Long -> MaterialSnackbarDuration.Long
                            SnackbarDuration.Indefinite -> MaterialSnackbarDuration.Indefinite
                        }
                    }
                )
                if (result == SnackbarResult.ActionPerformed) {
                    snackbarData.action()
                }
            }
        }
    }

    val dismissSnackbarState = rememberSwipeToDismissBoxState()
    val extraBottomPadding = bottomSpacing
        ?.let { (it - SnackbarOuterPadding).coerceAtLeast(0.dp) }
        ?: 0.dp

    SwipeToDismissBox(
        modifier = Modifier.fillMaxWidth(),
        state = dismissSnackbarState,
        backgroundContent = {},
        onDismiss = {
            hostState.currentSnackbarData?.dismiss()
            scope.launch {
                delay(SnackbarDelay)
                dismissSnackbarState.reset()
            }
        }
    ) {
        MaterialSnackbarHost(
            hostState = hostState,
        ) { data ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .thenIf(UiTestFlags.enabled) {
                        // Expose the message text to the accessibility tree so Maestro can
                        // reliably assert on it (the raw Snackbar text is not always visible
                        // to the automation driver), and allow tap-to-dismiss: swipe-to-dismiss
                        // gestures are unreliable to drive from the iOS automation driver.
                        Modifier
                            .semantics { contentDescription = data.visuals.message }
                            .clickable { data.dismiss() }
                    }
                    // Only pad the shown snackbar, an empty host must not take space or touches.
                    .padding(bottom = extraBottomPadding),
                contentAlignment = Alignment.Center
            ) {
                Snackbar(
                    snackbarData = data,
                    containerColor = LocalAppColors.current.dark,
                    contentColor = LocalAppColors.current.light,
                    actionColor = LocalAppColors.current.light,
                    actionContentColor = LocalAppColors.current.light,
                    dismissActionContentColor = LocalAppColors.current.light,
                )
            }
        }
    }
}

// A small delay for better animation
private val SnackbarDelay = 50.milliseconds

// The Material snackbar pads itself by 12dp on every side.
private val SnackbarOuterPadding = 12.dp

@Preview
@Composable
private fun SnackbarHost_Preview() = AppTheme {
    SnackbarHost(SnackbarData(message = "Update is ready!", actionLabel = "Continue"))
}