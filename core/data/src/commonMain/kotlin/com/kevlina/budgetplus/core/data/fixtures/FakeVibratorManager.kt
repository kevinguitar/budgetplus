package com.kevlina.budgetplus.core.data.fixtures

import androidx.annotation.RestrictTo
import com.kevlina.budgetplus.core.common.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeVibratorManager(
    initialVibrateOnInput: Boolean = true,
) : VibratorManager {

    override val vibrateOnInput: StateFlow<Boolean>
        field = MutableStateFlow(initialVibrateOnInput)

    override fun toggleVibrateOnInput() {
        vibrateOnInput.value = !vibrateOnInput.value
    }
}