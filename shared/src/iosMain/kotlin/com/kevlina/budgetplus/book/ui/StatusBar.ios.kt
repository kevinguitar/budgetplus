package com.kevlina.budgetplus.book.ui

import coil3.PlatformContext
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

actual fun setStatusBarColor(context: PlatformContext, isLightBg: Boolean) {
    val statusBarStyle = if (isLightBg) {
        UIStatusBarStyleDarkContent
    } else {
        UIStatusBarStyleLightContent
    }
    UIApplication.sharedApplication.setStatusBarStyle(statusBarStyle, animated = true)
}