package com.kevlina.budgetplus.book.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_group_add
import budgetplus.core.common.generated.resources.ic_settings
import com.kevlina.budgetplus.core.common.languageSelectionState
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BottomNavTab
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.theme.withTypographyScale
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.FontSize
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.LocalMenuActionTint
import com.kevlina.budgetplus.core.ui.MenuAction
import com.kevlina.budgetplus.core.ui.Text
import com.kevlina.budgetplus.core.ui.safeArea
import org.jetbrains.compose.resources.vectorResource

private val RailWidth = 80.dp
private val TabSpacing = 16.dp
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
        verticalArrangement = Arrangement.spacedBy(TabSpacing, Alignment.CenterVertically),
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
        BottomNavTab.entries.forEach { tab ->
            RailItem(navController = navController, tab = tab, colors = colors)
        }
    }
}

/**
 *  The label below the icon, the pill only wraps the icon.
 */
@Composable
private fun RailItem(
    navController: NavController<BookDest>,
    tab: BottomNavTab,
    colors: SideBarColors,
) {
    val isSelected = navController.isTabSelected(tab)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navTabClick(navController, tab)
            .padding(vertical = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            NavTabPill(
                isSelected = isSelected,
                color = colors.pill,
                size = SidePillSize,
            )
            // Keep the space of the pill while it animates.
            Spacer(modifier = Modifier.size(SidePillSize))
            Icon(
                imageVector = tab.icon(),
                tint = if (isSelected) colors.selectedIcon else colors.unselectedIcon,
                size = SideIconSize,
            )
        }

        // The rail stays on screen while the language is changed in the settings.
        key(languageSelectionState.value) {
            Text(
                text = tab.label(),
                color = colors.unselectedIcon,
                fontSize = FontSize.Small,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                singleLine = true,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 *  The tabs and the top bar actions in the column the system reserves on one side, e.g. for the
 *  status bar and the camera on the iPhone Duo. The items are centered vertically: depending on the
 *  orientation, the status bar and the camera can be at either end of the column.
 *
 *  @param isStart whether the column is on the start side, the separator goes on the content side.
 *  @param actions the actions of the current screen's top bar.
 *  @param width the width of the reserved column, it's filled exactly.
 */
@Composable
internal fun SideColumnBar(
    navController: NavController<BookDest>,
    previewColors: ThemeColors?,
    isStart: Boolean,
    actions: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    width: Dp = sideColumnWidth(),
) {
    val colors = sideBarColors(previewColors)
    // Keep showing the last actions while they animate out.
    val lastActions = remember { ActionsHolder() }
    if (actions != null) {
        lastActions.actions = actions
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .background(colors.background)
            .contentSeparator(isStart = isStart, color = colors.separator)
            .windowInsetsPadding(WindowInsets.safeArea.only(WindowInsetsSides.Vertical))
            .padding(vertical = 8.dp)
    ) {
        // Expand and shrink the actions, so the tabs slide to the new center instead of jumping.
        AnimatedVisibility(
            visible = actions != null,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    // The gap to the tabs is part of the animated content.
                    .padding(bottom = 8.dp)
                    // Screens can have a different number of actions.
                    .animateContentSize()
            ) {
                // The actions are made for the dark top bar.
                CompositionLocalProvider(LocalMenuActionTint provides colors.unselectedIcon) {
                    lastActions.actions?.invoke()
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
        }

        NavTabs(navController = navController, colors = colors)
    }
}

/**
 *  Not a state, it's only updated when the actions change, which recomposes the bar anyway.
 */
private class ActionsHolder {
    var actions: (@Composable () -> Unit)? = null
}

/**
 *  Only one side has an inset in the side column layout, see [rememberBookNavLayout].
 */
@Composable
private fun sideColumnWidth(): Dp {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeArea
    return with(density) {
        maxOf(insets.getLeft(density, layoutDirection), insets.getRight(density, layoutDirection)).toDp()
    }
}

@Composable
private fun NavTabs(
    navController: NavController<BookDest>,
    colors: SideBarColors,
) {
    // Same as the rail, the fixed pill and icon sizes fit the 84pt column on the iPhone Duo.
    Column(verticalArrangement = Arrangement.spacedBy(TabSpacing)) {
        BottomNavTab.entries.forEach { tab ->
            RailItem(navController = navController, tab = tab, colors = colors)
        }
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
    val light = previewColors?.light ?: LocalAppColors.current.light
    val dark = previewColors?.dark ?: LocalAppColors.current.dark
    return SideBarColors(
        background = light,
        pill = dark,
        selectedIcon = light,
        unselectedIcon = dark,
        separator = dark,
    )
}

@Preview(heightDp = 480)
@Composable
private fun NavRail_Preview() = AppTheme {
    NavRail(navController = NavController.preview, previewColors = null)
}

@Preview(heightDp = 480)
@Composable
private fun SideColumnBar_Preview() = AppTheme {
    SideColumnBar(
        navController = NavController.preview,
        previewColors = null,
        isStart = false,
        actions = {
            MenuAction(imageVector = vectorResource(Res.drawable.ic_group_add), description = "")
            MenuAction(imageVector = vectorResource(Res.drawable.ic_settings), description = "")
        },
        // The width of the column on the iPhone Duo, previews have no safe area.
        width = 84.dp,
    )
}

@Preview(heightDp = 480)
@Composable
private fun SideColumnBarNoActions_Preview() = AppTheme(themeColors = ThemeColors.AzureCoast) {
    SideColumnBar(
        navController = NavController.preview,
        previewColors = null,
        isStart = true,
        actions = null,
        width = 84.dp,
    )
}

