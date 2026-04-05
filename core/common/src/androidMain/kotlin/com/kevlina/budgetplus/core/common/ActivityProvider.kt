package com.kevlina.budgetplus.core.common

import android.app.Activity
import android.app.Application
import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.StateFlow

/**
 * Get the current resumed activity, useful for lifecycle ViewModel where you cannot inject Activity.
 */
interface ActivityProvider : Application.ActivityLifecycleCallbacks {
    val activityFlow: StateFlow<Activity?>

    // Make it nonnull later
    val currentActivity: ComponentActivity?
}
