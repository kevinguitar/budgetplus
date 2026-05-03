package com.kevlina.budgetplus.core.ui.bubble

import androidx.annotation.RestrictTo
import kotlinx.coroutines.flow.MutableStateFlow

@RestrictTo(RestrictTo.Scope.TESTS)
class FakeBubbleRepo : BubbleRepo {

    override val bubble = MutableStateFlow<BubbleDest?>(null)

    override suspend fun addBubbleToQueue(dest: BubbleDest) {
        error("Not yet implemented")
    }

    override fun popBubble() {
        error("Not yet implemented")
    }
}