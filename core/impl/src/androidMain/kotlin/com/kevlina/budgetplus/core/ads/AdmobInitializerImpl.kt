package com.kevlina.budgetplus.core.ads

import app.lexilabs.basic.ads.BasicAds
import com.kevlina.budgetplus.core.common.ActivityProvider
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.AppStartAction
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@ContributesIntoSet(AppScope::class, binding = binding<AppStartAction>())
@ContributesBinding(AppScope::class, binding = binding<AdmobInitializer>())
class AdmobInitializerImpl(
    private val activityProvider: ActivityProvider,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : AppStartAction, AdmobInitializer {

    override fun onAppStart() {
        appScope.launch {
            val activity = activityProvider.activityFlow
                .filterNotNull()
                .first()

            @Suppress("DEPRECATION")
            BasicAds.initialize(activity)
        }
    }

    // Not applicable to Android
    override fun requestTrackingAuthorization() = Unit
}