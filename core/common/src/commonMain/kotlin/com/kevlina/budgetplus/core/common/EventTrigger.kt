package com.kevlina.budgetplus.core.common

import androidx.compose.runtime.Stable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

@Stable
class EventTrigger<T> {

    val event: Flow<T>
        field = MutableSharedFlow<T>(
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            extraBufferCapacity = 1
        )

    fun sendEvent(content: T) {
        event.tryEmit(content)
    }
}