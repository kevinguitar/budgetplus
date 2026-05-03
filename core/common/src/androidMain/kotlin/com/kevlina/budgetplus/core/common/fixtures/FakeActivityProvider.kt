package com.kevlina.budgetplus.core.common.fixtures

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.common.ActivityProvider
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeActivityProvider(
    override val currentActivity: ComponentActivity,
) : ActivityProvider {
    override val activityFlow = MutableStateFlow(currentActivity)

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
}