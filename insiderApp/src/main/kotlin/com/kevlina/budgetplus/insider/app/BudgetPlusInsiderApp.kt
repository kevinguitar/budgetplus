package com.kevlina.budgetplus.insider.app

import android.app.Application
import co.touchlab.kermit.LogcatWriter
import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.di.HasServiceProvider
import com.kevlina.budgetplus.insiderApp.BuildConfig
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import dev.gitlive.firebase.crashlytics.crashlytics
import dev.zacsweers.metro.createGraphFactory

class BudgetPlusInsiderApp : Application(), HasServiceProvider {

    private val appGraph by lazy {
        createGraphFactory<BudgetPlusInsiderAppGraph.Factory>().create(this)
    }

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(appGraph.activityProvider)

        Logger.setLogWriters(LogcatWriter())
        Firebase.analytics.setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)
        Firebase.crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> resolve(): T = appGraph as T
}