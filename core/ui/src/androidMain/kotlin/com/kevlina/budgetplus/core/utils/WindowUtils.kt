package com.kevlina.budgetplus.core.utils

import android.app.Activity
import androidx.core.view.WindowCompat

fun Activity.setStatusBarColor(isLightBg: Boolean) {
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = isLightBg
    }
}