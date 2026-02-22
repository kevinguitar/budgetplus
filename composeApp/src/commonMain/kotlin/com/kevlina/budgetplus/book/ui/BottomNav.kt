package com.kevlina.budgetplus.book.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.ic_format_list_bulleted
import budgetplus.core.common.generated.resources.ic_post_add
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.BottomNavTab
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import com.kevlina.budgetplus.core.ui.AppTheme
import com.kevlina.budgetplus.core.ui.Icon
import com.kevlina.budgetplus.core.ui.rippleClick
import org.jetbrains.compose.resources.vectorResource

@Composable
internal fun BottomNav(
    navController: NavController<BookDest>,
    previewColors: ThemeColors?,
) {
    val lightColor = previewColors?.light ?: LocalAppColors.current.light
    val darkColor = previewColors?.dark ?: LocalAppColors.current.dark

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = lightColor)
            .navigationBarsPadding()
            .height(50.dp)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .alpha(AppTheme.DIVIDER_ALPHA)
                .background(color = darkColor)
        )

        Row(
            modifier = Modifier
                .weight(1F)
                .fillMaxWidth()
        ) {
            BottomNavTab.entries.forEach { tab ->
                BottomNavItem(
                    navController = navController,
                    tab = tab,
                    isSelected = navController.rootStack.lastOrNull() == tab.root,
                    darkColor = darkColor,
                    lightColor = lightColor
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    navController: NavController<BookDest>,
    tab: BottomNavTab,
    isSelected: Boolean,
    darkColor: Color,
    lightColor: Color,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1F)
            .fillMaxHeight()
            .rippleClick { navController.selectRoot(tab.root) }
    ) {

        this@BottomNavItem.AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(),
            exit = scaleOut()
        ) {

            Spacer(
                modifier = Modifier
                    .size(width = 60.dp, height = 36.dp)
                    .background(
                        color = darkColor,
                        shape = CircleShape
                    )
            )
        }

        Icon(
            imageVector = when (tab) {
                BottomNavTab.Add -> vectorResource(Res.drawable.ic_post_add)
                BottomNavTab.History -> vectorResource(Res.drawable.ic_format_list_bulleted)
            },
            tint = if (isSelected) lightColor else darkColor,
            modifier = Modifier.size(28.dp)
        )
    }
}