package com.kevlina.budgetplus.core.common

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ActivityProvider>())
class ActivityProviderImpl : ActivityProvider, Application.ActivityLifecycleCallbacks {

    override val activityFlow: StateFlow<Activity?>
        field = MutableStateFlow(null)

    override val currentActivity: ComponentActivity?
        get() = activityFlow.value as? ComponentActivity ?: run {
            Logger.e(MissingActivityException()) { "Missing current activity" }
            null
        }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityFlow.value = activity
    }

    override fun onActivityStarted(activity: Activity) {
        activityFlow.value = activity
    }

    override fun onActivityResumed(activity: Activity) {
        activityFlow.value = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (activityFlow.value == activity) {
            activityFlow.value = null
        }
    }

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}

private class MissingActivityException : RuntimeException("Current activity is null")
