package com.kevlina.budgetplus.core.common

import platform.Foundation.NSUUID
import platform.UIKit.UIDevice

actual fun randomUUID(): String = NSUUID().UUIDString()

// Keep in sync with the deployment target of RecordWidgetExtension.
actual val isRecordWidgetSupported: Boolean = UIDevice.currentDevice.systemVersion
    .substringBefore('.')
    .toIntOrNull()
    .let { it != null && it >= 17 }
