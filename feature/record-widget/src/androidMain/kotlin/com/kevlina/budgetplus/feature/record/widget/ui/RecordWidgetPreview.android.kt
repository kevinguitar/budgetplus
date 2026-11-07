package com.kevlina.budgetplus.feature.record.widget.ui

import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource

/**
 * Mirror the system colors that back the default Glance colors (`glance_color*` in Glance's
 * `values-v31` and `values-night-v31`), so the preview matches the widget on the home screen.
 */
@Composable
internal actual fun rememberDynamicWidgetPreviewColors(): WidgetPreviewColors? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return if (isSystemInDarkTheme()) dynamicDarkColors() else dynamicLightColors()
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun dynamicLightColors() = WidgetPreviewColors(
    background = color(android.R.color.system_accent2_50), // widgetBackground
    headerBackground = color(android.R.color.system_accent1_600), // primary
    headerText = color(android.R.color.system_accent1_0), // onPrimary
    itemText = color(android.R.color.system_neutral1_900), // onSurface
    pillBackground = color(android.R.color.system_accent2_100), // secondaryContainer
    pillText = color(android.R.color.system_accent2_900), // onSecondaryContainer
    divider = color(android.R.color.system_neutral2_100), // surfaceVariant
)

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun dynamicDarkColors() = WidgetPreviewColors(
    background = color(android.R.color.system_accent2_800),
    headerBackground = color(android.R.color.system_accent1_200),
    headerText = color(android.R.color.system_accent1_800),
    itemText = color(android.R.color.system_neutral1_100),
    pillBackground = color(android.R.color.system_accent2_700),
    pillText = color(android.R.color.system_accent2_100),
    divider = color(android.R.color.system_neutral2_700),
)

@Composable
private fun color(@ColorRes id: Int) = colorResource(id)
