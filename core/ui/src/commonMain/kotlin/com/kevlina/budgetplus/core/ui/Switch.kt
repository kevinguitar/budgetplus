package com.kevlina.budgetplus.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Adaptive switch.
 *
 * On Android it renders the app's themed, scaled Material [androidx.compose.material3.Switch]
 * (unchanged), while on iOS it renders Calf's native Cupertino switch tinted with
 * the app's theme color.
 */
@Composable
expect fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
)
