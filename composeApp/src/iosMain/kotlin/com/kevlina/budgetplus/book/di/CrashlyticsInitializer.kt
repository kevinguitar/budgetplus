package com.kevlina.budgetplus.book.di

import co.touchlab.crashkios.crashlytics.setCrashlyticsUnhandledExceptionHook
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.utils.CrashReportingLogWriter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet

@ContributesIntoSet(AppScope::class)
class CrashlyticsInitializer : AppStartAction {

    override fun onAppStart() {
        Logger.setLogWriters(CrashReportingLogWriter())
        setCrashlyticsUnhandledExceptionHook()
    }
}