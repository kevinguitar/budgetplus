package com.kevlina.budgetplus.core.ui

import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.mohamedrejeb.calf.ui.toggle.AdaptiveSwitch

@Composable
actual fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    // The native Cupertino switch uses the checked track color as its "on" tint.
    AdaptiveSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = LocalAppColors.current.dark,
        ),
        modifier = modifier,
    )
}
