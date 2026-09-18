@file:OptIn(com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi::class)

package com.kevlina.budgetplus.book.ui

import androidx.compose.runtime.Composable
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
import com.mohamedrejeb.calf.ui.navigation.AdaptiveNavigationBar
import com.mohamedrejeb.calf.ui.navigation.UIKitTabBarConfiguration
import com.mohamedrejeb.calf.ui.navigation.UIKitUITabBarItem
import com.mohamedrejeb.calf.ui.uikit.UIKitImage
import org.jetbrains.compose.resources.vectorResource

@Composable
internal actual fun BottomNav(
    navController: NavController<BookDest>,
    previewColors: ThemeColors?,
) {
    val darkColor = previewColors?.dark ?: LocalAppColors.current.dark

    val tabs = BottomNavTab.entries
    val currentRoot = navController.rootStack.lastOrNull()
    val selectedIndex = tabs.indexOfFirst { it.root == currentRoot }.coerceAtLeast(0)

    val addIcon = UIKitImage.Vector(vectorResource(Res.drawable.ic_post_add), 28.dp, 28.dp)
    val historyIcon = UIKitImage.Vector(vectorResource(Res.drawable.ic_format_list_bulleted), 28.dp, 28.dp)

    AdaptiveNavigationBar(
        iosItems = tabs.map { tab ->
            val icon = when (tab) {
                BottomNavTab.Add -> addIcon
                BottomNavTab.History -> historyIcon
            }
            UIKitUITabBarItem(title = "", image = icon)
        },
        iosSelectedIndex = selectedIndex,
        iosOnItemSelected = { index ->
            tabs.getOrNull(index)?.let { navController.selectRoot(it.root) }
        },
        iosConfiguration = UIKitTabBarConfiguration(
            selectedItemColor = darkColor,
            unselectedItemColor = darkColor.copy(alpha = 0.5f),
        ),
    )
}
