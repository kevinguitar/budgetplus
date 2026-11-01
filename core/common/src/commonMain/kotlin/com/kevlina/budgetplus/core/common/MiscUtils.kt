package com.kevlina.budgetplus.core.common

expect fun randomUUID(): String

/**
 * Whether the OS supports the record widget. iOS: the widget extension requires iOS 17.
 */
expect val isRecordWidgetSupported: Boolean
