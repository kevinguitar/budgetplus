package com.kevlina.budgetplus.core.common

import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.StateFlow

/**
 * Get the current resumed activity, useful for lifecycle ViewModel where you cannot inject Activity.
 */
interface ActivityProvider {
    val activityFlow: StateFlow<ComponentActivity?>

    // Make it nonnull later
    val currentActivity: ComponentActivity?
}
