@file:OptIn(com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kevlina.budgetplus.book.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import platform.UIKit.UIColor
import platform.UIKit.UITabBar
import platform.UIKit.UITabBarAppearance

@Composable
internal actual fun BottomNav(
    navController: NavController<BookDest>,
    previewColors: ThemeColors?,
) {
    val lightColor = previewColors?.light ?: LocalAppColors.current.light
    val darkColor = previewColors?.dark ?: LocalAppColors.current.dark

    // Force a solid light background on the native UITabBar. Calf does not expose a
    // container color, so we configure the global UITabBar appearance proxy; new tab
    // bars (including Calf's) pick it up.
    LaunchedEffect(lightColor) {
        val uiLight = lightColor.toUIColor()
        val appearance = UITabBarAppearance().apply {
            configureWithOpaqueBackground()
            backgroundColor = uiLight
        }
        UITabBar.appearance().apply {
            standardAppearance = appearance
            scrollEdgeAppearance = appearance
            backgroundColor = uiLight
        }
    }

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
            unselectedItemColor = darkColor,
            isTranslucent = false,
        ),
    )
}

private fun Color.toUIColor(): UIColor {
    val argb = toArgb()
    val a = ((argb shr 24) and 0xFF) / 255.0
    val r = ((argb shr 16) and 0xFF) / 255.0
    val g = ((argb shr 8) and 0xFF) / 255.0
    val b = (argb and 0xFF) / 255.0
    return UIColor(red = r, green = g, blue = b, alpha = a)
}
