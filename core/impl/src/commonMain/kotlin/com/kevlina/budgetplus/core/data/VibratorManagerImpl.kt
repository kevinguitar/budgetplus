package com.kevlina.budgetplus.core.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.Tracker
import com.kevlina.budgetplus.core.common.VibratorManager
import com.kevlina.budgetplus.core.data.local.Preference
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class VibratorManagerImpl(
    private val preference: Preference,
    private val tracker: Tracker,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : VibratorManager {

    private val vibrateOnInputKey = booleanPreferencesKey("vibrateOnInputFlow")

    override val vibrateOnInput: StateFlow<Boolean> = preference.of(vibrateOnInputKey, true, appScope)

    override fun toggleVibrateOnInput() {
        appScope.launch { preference.update(vibrateOnInputKey, !vibrateOnInput.value) }
        tracker.logEvent("vibrate_on_input_changed")
    }
}