package com.kevlina.budgetplus.book.ui

import android.app.Activity
import coil3.PlatformContext
import com.kevlina.budgetplus.core.utils.setStatusBarColor

actual fun setStatusBarColor(context: PlatformContext, isLightBg: Boolean) {
    (context as Activity).setStatusBarColor(isLightBg)
}