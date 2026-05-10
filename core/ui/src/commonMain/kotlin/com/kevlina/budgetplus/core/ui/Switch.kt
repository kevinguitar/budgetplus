package com.kevlina.budgetplus.core.ui

import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import com.kevlina.budgetplus.core.theme.LocalAppColors
import com.kevlina.budgetplus.core.theme.ThemeColors
import androidx.compose.material3.Switch as MaterialSwitch

private const val SWITCH_SCALE = 0.7F

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = LocalAppColors.current.light,
            checkedTrackColor = LocalAppColors.current.dark,
            uncheckedThumbColor = LocalAppColors.current.dark,
            uncheckedTrackColor = LocalAppColors.current.light,
            uncheckedBorderColor = LocalAppColors.current.dark
        ),
        modifier = modifier.scale(SWITCH_SCALE),
    )
}

@Preview
@Composable
private fun Switch_Preview() = AppTheme(themeColors = ThemeColors.NemoSea) {
    var checked by remember { mutableStateOf(false) }
    Switch(checked = checked, onCheckedChange = { checked = !checked })
}