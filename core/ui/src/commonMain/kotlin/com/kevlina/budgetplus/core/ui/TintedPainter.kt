package com.kevlina.budgetplus.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

/**
 * Remembers a [TintedPainter] wrapping [painter] with the given [tint].
 */
@Composable
fun rememberTintedPainter(painter: Painter, tint: Color): Painter =
    remember(painter, tint) { TintedPainter(painter, tint) }

/**
 * Wraps a [Painter] and draws it with the given [tint] applied via [ColorFilter.tint].
 */
private class TintedPainter(
    private val delegate: Painter,
    private val tint: Color,
) : Painter() {

    override val intrinsicSize: Size get() = delegate.intrinsicSize

    override fun DrawScope.onDraw() {
        with(delegate) {
            draw(size = size, colorFilter = ColorFilter.tint(tint))
        }
    }
}