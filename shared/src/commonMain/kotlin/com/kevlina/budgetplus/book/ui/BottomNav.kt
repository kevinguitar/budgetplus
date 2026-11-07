package com.kevlina.budgetplus.book.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_format_list_bulleted
import budgetplus.core.common.generated.resources.ic_post_add
import com.kevlina.budgetplus.core.common.UiTestFlags
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BottomNavTab
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.theme.withTypographyScale
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.rippleClick
import com.kevlina.budgetplus.core.ui.safeArea
import com.kevlina.budgetplus.core.ui.thenIf
import org.jetbrains.compose.resources.vectorResource

private const val BOTTOM_NAV_ADD_DESC = "bottom_nav_add"
private const val BOTTOM_NAV_HISTORY_DESC = "bottom_nav_history"
private val DividerHeight = 1.dp

@Composable
internal fun BottomNav(
    navController: NavController<BookDest>,
    previewColors: ThemeColors?,
) {
    val lightColor = previewColors?.light ?: LocalAppColors.current.light
    val darkColor = previewColors?.dark ?: LocalAppColors.current.dark

    // The background and the divider go edge to edge, the tabs avoid the home indicator and side cutouts.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = lightColor)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(DividerHeight)
                .alpha(AppTheme.DIVIDER_ALPHA)
                .background(color = darkColor)
        )

        Row(
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.safeArea.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .height(50.dp.withTypographyScale() - DividerHeight)
                .fillMaxWidth()
        ) {
            BottomNavTab.entries.forEach { tab ->
                NavTabItem(
                    navController = navController,
                    tab = tab,
                    pillColor = darkColor,
                    selectedIconColor = lightColor,
                    unselectedIconColor = darkColor,
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxHeight()
                )
            }
        }
    }
}

/**
 *  A tab of the book navigation, shared by the bottom nav and the side bars.
 */
@Composable
internal fun NavTabItem(
    navController: NavController<BookDest>,
    tab: BottomNavTab,
    pillColor: Color,
    selectedIconColor: Color,
    unselectedIconColor: Color,
    modifier: Modifier = Modifier,
) {
    val isSelected = navController.rootStack.lastOrNull() == tab.root
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .rippleClick { navController.selectRoot(tab.root) }
            .thenIf(UiTestFlags.enabled) {
                Modifier.semantics {
                    contentDescription = when (tab) {
                        BottomNavTab.Add -> BOTTOM_NAV_ADD_DESC
                        BottomNavTab.History -> BOTTOM_NAV_HISTORY_DESC
                    }
                }
            }
    ) {
        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            Spacer(
                modifier = Modifier
                    .size(
                        width = 60.dp.withTypographyScale(),
                        height = 36.dp.withTypographyScale()
                    )
                    .background(
                        color = pillColor,
                        shape = CircleShape
                    )
            )
        }

        Icon(
            imageVector = when (tab) {
                BottomNavTab.Add -> vectorResource(Res.drawable.ic_post_add)
                BottomNavTab.History -> vectorResource(Res.drawable.ic_format_list_bulleted)
            },
            tint = if (isSelected) selectedIconColor else unselectedIconColor,
            size = 28.dp,
        )
    }
}