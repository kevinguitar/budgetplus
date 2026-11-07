package com.kevlina.budgetplus.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 *  Hosts the [TopBar] menu actions outside the top bar, e.g. in the vertical bar on the iPhone Duo.
 *
 *  Every [TopBar] registers its actions while it's in the composition, even if it has none, so a
 *  screen without actions doesn't show the actions of the previous screen. During navigation
 *  transitions more than one screen is composed, the latest registered one wins.
 */
@Stable
class TopBarActionsHost {

    private val entries = mutableStateListOf<Entry>()

    /**
     *  The actions of the current screen, null if it has none.
     */
    val actions: (@Composable () -> Unit)?
        get() = entries.lastOrNull()?.content

    internal fun register(entry: Entry) {
        entries.add(entry)
    }

    internal fun unregister(entry: Entry) {
        entries.remove(entry)
    }

    internal class Entry(val content: (@Composable () -> Unit)?)
}

/**
 *  Null when the actions should stay in the [TopBar].
 */
val LocalTopBarActionsHost = staticCompositionLocalOf<TopBarActionsHost?> { null }

/**
 *  Registers [menuActions] to [host] while this is in the composition.
 */
@Composable
internal fun RegisterTopBarActions(
    host: TopBarActionsHost,
    menuActions: (@Composable () -> Unit)?,
) {
    // Keep the entry stable across recompositions, but always render the latest actions, e.g. the
    // sorting action changes its icon with the sort mode.
    val latestActions by rememberUpdatedState(menuActions)
    val hasActions = menuActions != null
    DisposableEffect(host, hasActions) {
        val entry = TopBarActionsHost.Entry(
            content = if (hasActions) {
                { latestActions?.invoke() }
            } else {
                null
            }
        )
        host.register(entry)
        onDispose { host.unregister(entry) }
    }
}
