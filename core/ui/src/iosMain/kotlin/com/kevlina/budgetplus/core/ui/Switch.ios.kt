@file:OptIn(com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi::class)

package com.kevlina.budgetplus.core.ui

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.mohamedrejeb.calf.ui.toggle.CupertinoSwitch
import com.mohamedrejeb.calf.ui.toggle.LiquidGlassSwitch
import com.mohamedrejeb.calf.ui.toggle.LiquidGlassSwitchDefaults
import androidx.compose.material3.SwitchDefaults
import platform.UIKit.UIDevice

@Composable
actual fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val dark = LocalAppColors.current.dark
    val light = LocalAppColors.current.light

    // The native Cupertino switch (31dp) and the Liquid Glass switch (28dp) are
    // shorter than the Material switch's 48dp interactive size, which made switch
    // rows collapse. Reserve the same height so rows stay aligned with other cells.
    val sizedModifier = modifier
        .defaultMinSize(minHeight = 48.dp)
        .wrapContentHeight()

    // AdaptiveSwitch ignores the passed colors on iOS 26 (it hardcodes the green
    // Liquid Glass defaults), so we call the platform switch directly and force the
    // app theme colors for the "on" state.
    if (isIOS26OrAbove()) {
        LiquidGlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = LiquidGlassSwitchDefaults.colors(
                checkedTrackColor = dark,
            ),
            modifier = sizedModifier,
        )
    } else {
        CupertinoSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = light,
                checkedTrackColor = dark,
                uncheckedThumbColor = light,
                uncheckedTrackColor = dark.copy(alpha = 0.3f),
            ),
            modifier = sizedModifier,
        )
    }
}

private fun isIOS26OrAbove(): Boolean {
    val major = UIDevice.currentDevice.systemVersion
        .substringBefore('.')
        .toIntOrNull()
        ?: 0
    return major >= 26
}
