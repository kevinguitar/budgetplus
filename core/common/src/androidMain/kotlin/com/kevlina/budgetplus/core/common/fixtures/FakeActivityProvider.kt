package com.kevlina.budgetplus.core.common.fixtures

import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting
import com.kevlina.budgetplus.core.common.ActivityProvider
import kotlinx.coroutines.flow.MutableStateFlow

@VisibleForTesting
class FakeActivityProvider(
    override val currentActivity: ComponentActivity,
) : ActivityProvider {
    override val activityFlow = MutableStateFlow(currentActivity)
}