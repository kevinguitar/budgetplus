package com.kevlina.budgetplus.feature.record.widget

import android.os.Build

internal actual val isRecordWidgetDynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
