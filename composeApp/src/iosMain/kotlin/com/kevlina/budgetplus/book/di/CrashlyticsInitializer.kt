package com.kevlina.budgetplus.book.di

import co.touchlab.crashkios.crashlytics.setCrashlyticsUnhandledExceptionHook
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.utils.CrashReportingLogWriter

object CrashlyticsInitializer {
    fun initialize() {
        Logger.setLogWriters(CrashReportingLogWriter())
        setCrashlyticsUnhandledExceptionHook()
    }
}