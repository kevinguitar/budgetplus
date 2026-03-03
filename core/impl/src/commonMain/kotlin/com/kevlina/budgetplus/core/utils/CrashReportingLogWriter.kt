package com.kevlina.budgetplus.core.utils

import co.touchlab.crashkios.crashlytics.CrashlyticsCalls
import co.touchlab.crashkios.crashlytics.CrashlyticsCallsActual
import co.touchlab.crashkios.crashlytics.enableCrashlytics
import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Message
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import kotlinx.coroutines.CancellationException

class CrashReportingLogWriter : LogWriter() {

    private val crashlyticsCalls: CrashlyticsCalls = CrashlyticsCallsActual()

    init {
        enableCrashlytics()
    }

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        crashlyticsCalls.logMessage(DefaultFormatter.formatMessage(severity, Tag(tag), Message(message)))

        if (severity >= Severity.Error && throwable !is CancellationException) {
            // If throwable is null, wrap the message with an exception
            crashlyticsCalls.sendHandledException(throwable ?: Exception(message))
        }
    }
}
