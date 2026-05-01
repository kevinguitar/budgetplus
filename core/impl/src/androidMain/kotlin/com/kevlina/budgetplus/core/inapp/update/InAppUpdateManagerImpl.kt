package com.kevlina.budgetplus.core.inapp.update

import androidx.activity.ComponentActivity
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.google.android.play.core.ktx.requestUpdateFlow
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.inapp.update.InAppUpdateManagerImpl.Companion.DAYS_FOR_FLEXIBLE_UPDATE
import com.kevlina.budgetplus.core.inapp.update.InAppUpdateManagerImpl.Companion.DAYS_FOR_IMMEDIATE_UPDATE
import com.kevlina.budgetplus.inapp.update.InAppUpdateManager
import com.kevlina.budgetplus.inapp.update.InAppUpdateState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@ContributesBinding(AppScope::class)
internal class InAppUpdateManagerImpl(
    private val activity: ComponentActivity,
    private val tracker: Tracker,
    private val preference: Preference,
) : InAppUpdateManager {

    override val updateState: MutableStateFlow<InAppUpdateState> = MutableStateFlow(InAppUpdateState.NotStarted)

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
    private val scope = activity.lifecycleScope

    // The version code when we requested the update last time.
    private val lastRequestedVersionCodeKey = intPreferencesKey("lastRequestedVersionCode")
    private val lastRequestedVersionCode = preference.of(lastRequestedVersionCodeKey)

    init {
        appUpdateManager.requestUpdateFlow()
            .onEach(::processResult)
            .catch { e -> Logger.e(e) { "AppUpdate flow failed" } }
            .launchIn(scope)
    }

    private suspend fun processResult(result: AppUpdateResult) {
        when (result) {
            is AppUpdateResult.Available -> {
                val updateInfo = result.updateInfo
                val shouldRequestImmediateUpdate = with(updateInfo) {
                    updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && isImmediateUpdateAllowed
                        && (clientVersionStalenessDays() ?: 0) >= DAYS_FOR_IMMEDIATE_UPDATE
                        && availableVersionCode() > (lastRequestedVersionCode.first() ?: 0)
                }

                val shouldRequestFlexibleUpdate = with(updateInfo) {
                    updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && isFlexibleUpdateAllowed
                        && (clientVersionStalenessDays() ?: 0) >= DAYS_FOR_FLEXIBLE_UPDATE
                        && availableVersionCode() > (lastRequestedVersionCode.first() ?: 0)
                }

                when {
                    shouldRequestImmediateUpdate -> {
                        val availableVersion = updateInfo.availableVersionCode()
                        preference.update(lastRequestedVersionCodeKey, availableVersion)
                        result.startImmediateUpdate(activity, REQ_APP_UPDATE)
                        tracker.logEvent("inapp_update_immediate")
                    }

                    shouldRequestFlexibleUpdate -> {
                        val availableVersion = updateInfo.availableVersionCode()
                        preference.update(lastRequestedVersionCodeKey, availableVersion)
                        result.startFlexibleUpdate(activity, REQ_APP_UPDATE)
                        tracker.logEvent("inapp_update_flexible")
                    }
                }
            }

            is AppUpdateResult.InProgress -> {
                updateState.value = InAppUpdateState.Downloading
            }

            is AppUpdateResult.Downloaded -> {
                updateState.value = InAppUpdateState.Downloaded {
                    completeUpdate(result)
                }
            }

            AppUpdateResult.NotAvailable -> {
                updateState.value = InAppUpdateState.NotAvailable
            }
        }
    }

    private fun completeUpdate(result: AppUpdateResult.Downloaded) {
        scope.launch {
            try {
                result.completeUpdate()
                tracker.logEvent("inapp_update_flexible_complete")
            } catch (e: Exception) {
                Logger.e(e) { "Fail to complete flexible update" }
            }
        }
    }

    /**
     *  If the latest version is already available on Google Play for [DAYS_FOR_FLEXIBLE_UPDATE]
     *  days, then request a flexible update. If it's already available more than
     *  [DAYS_FOR_IMMEDIATE_UPDATE] days, then request a immediate update.
     */
    private companion object {
        const val DAYS_FOR_FLEXIBLE_UPDATE = 3
        const val DAYS_FOR_IMMEDIATE_UPDATE = 30
        const val REQ_APP_UPDATE = 4820
    }
}