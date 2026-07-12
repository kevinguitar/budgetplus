package com.kevlina.budgetplus.book.di

import co.touchlab.crashkios.crashlytics.setCrashlyticsUnhandledExceptionHook
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.utils.CrashReportingLogWriter
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics

@Suppress("unused") // Called from iOSApp.swift
object CrashlyticsInitializer {
    fun initialize() {
        Logger.setLogWriters(CrashReportingLogWriter())
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(true)
        setCrashlyticsUnhandledExceptionHook()
    }
}