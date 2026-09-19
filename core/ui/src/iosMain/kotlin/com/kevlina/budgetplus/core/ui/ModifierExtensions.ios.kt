package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import com.mohamedrejeb.calf.ui.gesture.adaptiveClickable

actual fun Modifier.rippleClick(
    color: Color,
    borderless: Boolean,
    onLongClick: (() -> Unit)?,
    onClick: () -> Unit,
) = this.composed {
    if (onLongClick == null) {
        // Native iOS scaling tap effect.
        adaptiveClickable(onClick = onClick)
    } else {
        // adaptiveClickable has no long-press support; keep the combined behavior.
        // see: https://github.com/MohamedRejeb/Calf/pull/549
        combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = !borderless, color = color),
            onClick = onClick,
            onLongClick = onLongClick
        )
    }
}
