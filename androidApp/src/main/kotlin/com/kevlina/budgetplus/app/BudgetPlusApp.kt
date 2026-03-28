package com.kevlina.budgetplus.app

import android.app.Application
import co.touchlab.kermit.LogcatWriter
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.ActivityProviderImpl
import com.kevlina.budgetplus.core.common.AppStartAction
import com.kevlina.budgetplus.core.common.di.HasServiceProvider
import com.kevlina.budgetplus.core.utils.CrashReportingLogWriter
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.crashlytics.crashlytics
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.createGraphFactory

class BudgetPlusApp : Application(), HasServiceProvider {

    @Inject lateinit var activityProvider: ActivityProviderImpl
    @Inject lateinit var appStartActions: Set<AppStartAction>
    @Inject @Named("is_debug") var isDebug: Boolean = false

    private val appGraph by lazy {
        createGraphFactory<BudgetPlusAppGraph.Factory>().create(this)
    }

    override fun onCreate() {
        appGraph.inject(this)
        super.onCreate()

        registerActivityLifecycleCallbacks(activityProvider)

        Logger.setLogWriters(if (isDebug) LogcatWriter() else CrashReportingLogWriter())
        Firebase.analytics.setAnalyticsCollectionEnabled(!isDebug)
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(!isDebug)

        // Execute all the actions that need to be executed on app start.
        appStartActions.forEach { it.onAppStart() }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> resolve(): T = appGraph as T
}