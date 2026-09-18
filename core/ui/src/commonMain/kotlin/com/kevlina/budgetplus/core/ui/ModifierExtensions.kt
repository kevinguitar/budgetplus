package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color

inline fun Modifier.thenIf(condition: Boolean, modifierProvider: () -> Modifier): Modifier {
    return if (condition) {
        then(modifierProvider())
    } else {
        this
    }
}

inline fun <T> Modifier.thenIfNotNull(value: T?, modifierProvider: (T) -> Modifier): Modifier {
    return if (value != null) {
        then(modifierProvider(value))
    } else {
        this
    }
}

/**
 * A clickable modifier that provides a platform-adaptive tap feedback.
 *
 * On Android it shows the standard Material ripple (unchanged). On iOS it uses
 * Calf's `adaptiveClickable`, which replaces the ripple with the native iOS
 * scaling effect. When [onLongClick] is provided, the Android combined-click
 * behavior is used on both platforms (the native scale effect does not support
 * long-press).
 */
expect fun Modifier.rippleClick(
    color: Color = Color.Unspecified,
    borderless: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier

fun Modifier.clickableWithoutRipple(onClick: () -> Unit) = this.composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

fun Modifier.rippleIndication(
    interactionSource: MutableInteractionSource,
) = indication(
    interactionSource = interactionSource,
    indication = ripple()
)
