package com.kevlina.budgetplus.core.common

import java.util.*

actual fun randomUUID(): String = UUID.randomUUID().toString()

actual val isRecordWidgetSupported: Boolean = true
