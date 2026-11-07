package com.kevlina.budgetplus.book.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BottomNavTab
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.theme.withTypographyScale
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.LocalMenuActionTint
import com.kevlina.budgetplus.core.ui.safeArea

private val RailWidth = 80.dp
private val TabHeight = 48.dp
private val SidePillSize = DpSize(56.dp, 32.dp)
private val SideIconSize = 24.dp
private val SeparatorWidth = 1.dp

/**
 *  The tabs in a rail at the start, for wide windows.
 */
@Composable
internal fun NavRail(
    navController: NavController<BookDest>,
    previewColors: ThemeColors?,
    modifier: Modifier = Modifier,
) {
    val colors = sideBarColors(previewColors)
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .background(colors.background)
            .contentSeparator(isStart = true, color = colors.separator)
            .windowInsetsPadding(
                WindowInsets.safeArea.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)
            )
            .width(RailWidth.withTypographyScale())
    ) {
        NavTabs(navController = navController, colors = colors)
    }
}

/**
 *  The tabs and the top bar actions in the column the system reserves on one side, e.g. for the
 *  status bar and the camera on the iPhone Duo. The items are centered vertically: depending on the
 *  orientation, the status bar and the camera can be at either end of the column.
 *
 *  @param isStart whether the column is on the start side, the separator goes on the content side.
 *  @param actions the actions of the current screen's top bar.
 */
@Composable
internal fun SideColumnBar(
    navController: NavController<BookDest>,
    previewColors: ThemeColors?,
    isStart: Boolean,
    actions: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = sideBarColors(previewColors)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeArea
    // Fill the reserved column exactly. Only one side has an inset in this layout, see rememberBookNavLayout.
    val width = with(density) {
        maxOf(insets.getLeft(density, layoutDirection), insets.getRight(density, layoutDirection)).toDp()
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(colors.background)
            .contentSeparator(isStart = isStart, color = colors.separator)
            .windowInsetsPadding(WindowInsets.safeArea.only(WindowInsetsSides.Vertical))
            .padding(vertical = 8.dp)
    ) {
        if (actions != null) {
            // The actions are made for the dark top bar.
            CompositionLocalProvider(LocalMenuActionTint provides colors.unselectedIcon) {
                actions()
            }
            Spacer(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .alpha(AppTheme.DIVIDER_ALPHA)
                    .background(color = colors.unselectedIcon)
            )
        }

        NavTabs(navController = navController, colors = colors)
    }
}

@Composable
private fun ColumnScope.NavTabs(
    navController: NavController<BookDest>,
    colors: SideBarColors,
) {
    BottomNavTab.entries.forEach { tab ->
        NavTabItem(
            navController = navController,
            tab = tab,
            pillColor = colors.pill,
            selectedIconColor = colors.selectedIcon,
            unselectedIconColor = colors.unselectedIcon,
            modifier = Modifier
                .fillMaxWidth()
                .height(TabHeight),
            // Fit the narrow bar, e.g. the 84pt column on the iPhone Duo, regardless of the text scale.
            pillSize = SidePillSize,
            iconSize = SideIconSize,
        )
    }
}

/**
 *  A thin line on the edge that faces the content.
 */
private fun Modifier.contentSeparator(isStart: Boolean, color: Color): Modifier = drawBehind {
    val isLeft = isStart == (layoutDirection == LayoutDirection.Ltr)
    val strokeWidth = SeparatorWidth.toPx()
    val x = if (isLeft) size.width - strokeWidth / 2 else strokeWidth / 2
    drawLine(
        color = color,
        start = Offset(x, 0F),
        end = Offset(x, size.height),
        strokeWidth = strokeWidth,
        alpha = AppTheme.DIVIDER_ALPHA,
    )
}

@Immutable
private class SideBarColors(
    val background: Color,
    val pill: Color,
    val selectedIcon: Color,
    val unselectedIcon: Color,
    val separator: Color,
)

/**
 *  Same as the bottom nav, which the side bars replace.
 */
@Composable
private fun sideBarColors(previewColors: ThemeColors?): SideBarColors {
    val lightBg = previewColors?.lightBg ?: LocalAppColors.current.lightBg
    val dark = previewColors?.dark ?: LocalAppColors.current.dark
    return SideBarColors(
        background = lightBg,
        pill = dark,
        selectedIcon = lightBg,
        unselectedIcon = dark,
        separator = dark,
    )
}
