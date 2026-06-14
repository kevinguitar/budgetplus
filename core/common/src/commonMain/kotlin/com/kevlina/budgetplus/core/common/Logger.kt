package com.kevlina.budgetplus.core.common

import co.touchlab.kermit.Logger as KermitLogger

object Logger {
    fun d(message: String) = KermitLogger.d(message)

    fun d(t: Throwable, message: String) = KermitLogger.d(t) { message }

    fun i(message: String) = KermitLogger.i(message)

    fun w(message: String) = KermitLogger.w(message)

    fun w(t: Throwable, message: String) = KermitLogger.w(t) { message }

    fun e(message: String) = KermitLogger.e(message)

    fun e(t: Throwable, message: String) = KermitLogger.e(t) { message }
}