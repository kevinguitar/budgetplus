package com.kevlina.budgetplus.core.adaptive

import androidx.compose.runtime.Composable

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

data class WindowSizeClass(
    val width: Size,
    val height: Size,
    val isLandscape: Boolean = false,
) {

    /**
     *  Wide enough for a side by side layout. Compare the actual dimensions instead of the size
     *  classes, e.g. a 800x600dp window is Medium in both dimensions but still landscape.
     */
    val isWide: Boolean get() = width >= Size.Medium && isLandscape

    enum class Size {
        Compat, Medium, Expanded
    }

    companion object {
        @Composable
        fun calculate(): WindowSizeClass {
            val containerSize = LocalWindowInfo.current.containerSize
            val density = LocalDensity.current

            val widthDp = with(density) { containerSize.width.toDp() }
            val heightDp = with(density) { containerSize.height.toDp() }

            return WindowSizeClass(
                width = when {
                    widthDp < 720.dp -> Size.Compat
                    widthDp < 900.dp -> Size.Medium
                    else -> Size.Expanded
                },
                height = when {
                    heightDp < 480.dp -> Size.Compat
                    heightDp < 900.dp -> Size.Medium
                    else -> Size.Expanded
                },
                isLandscape = widthDp > heightDp,
            )
        }
    }
}