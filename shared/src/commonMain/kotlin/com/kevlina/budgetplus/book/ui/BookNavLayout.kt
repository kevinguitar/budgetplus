package com.kevlina.budgetplus.book.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.kevlina.budgetplus.core.adaptive.WindowSizeClass
import com.kevlina.budgetplus.core.ui.safeArea

/**
 *  Whether the platform reserves a column on one side of the window for system UI, the iPhone Duo
 *  puts its status bar and camera there. Android devices can have a cutout on one side too, but
 *  that's only a camera hole, so they keep the regular layouts.
 */
internal expect val hasSideColumnForSystemUi: Boolean

@Immutable
internal sealed interface BookNavLayout {

    /**
     *  Tabs at the bottom, actions in the top bar.
     */
    data object Bottom : BookNavLayout

    /**
     *  Tabs in a rail at the start, actions in the top bar. For wide windows.
     */
    data object Rail : BookNavLayout

    /**
     *  Tabs and actions in the column the system reserves on one side, see [hasSideColumnForSystemUi].
     *
     *  @param isStart whether the column is on the start side, in the layout direction.
     */
    data class SideColumn(val isStart: Boolean) : BookNavLayout
}

@Composable
internal fun rememberBookNavLayout(): BookNavLayout {
    val windowSizeClass = WindowSizeClass.calculate()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeArea
    val left = insets.getLeft(density, layoutDirection)
    val right = insets.getRight(density, layoutDirection)

    return when {
        // Symmetric insets, e.g. a landscape iPhone with the Dynamic Island, don't form a column.
        hasSideColumnForSystemUi && (left > 0) != (right > 0) -> {
            val isLeft = left > 0
            BookNavLayout.SideColumn(isStart = isLeft == (layoutDirection == LayoutDirection.Ltr))
        }

        windowSizeClass.isWide -> BookNavLayout.Rail
        else -> BookNavLayout.Bottom
    }
}
