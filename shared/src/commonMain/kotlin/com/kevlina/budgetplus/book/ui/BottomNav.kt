package com.kevlina.budgetplus.book.ui

import androidx.compose.runtime.Composable
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.core.theme.ThemeColors

/**
 * Adaptive bottom navigation.
 *
 * On Android it renders the app's custom material-like bottom bar with the
 * animated selection pill (unchanged). On iOS it renders Calf's
 * [com.mohamedrejeb.calf.ui.navigation.AdaptiveNavigationBar], a native
 * `UITabBar`.
 */
@Composable
internal expect fun BottomNav(
    navController: NavController<BookDest>,
    previewColors: ThemeColors?,
)
