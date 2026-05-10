package com.kevlina.budgetplus.core.ui

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.data.local.Preference
import com.kevlina.budgetplus.core.ui.bubble.BubbleDest
import com.kevlina.budgetplus.core.ui.bubble.BubbleRepo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
internal class BubbleRepoImpl(
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val preference: Preference,
) : BubbleRepo {

    private val bubblesQueue = mutableListOf<BubbleDest>()

    override val bubble: StateFlow<BubbleDest?>
        field = MutableStateFlow<BubbleDest?>(null)

    private var bubbleShownJob: Job? = null
    private var popBubbleJob: Job? = null

    private val queueMutex = Mutex()

    override suspend fun addBubbleToQueue(dest: BubbleDest) {
        queueMutex.withLock {
            val bubbleKey = booleanPreferencesKey(dest.key)
            if (preference.of(bubbleKey).first() == true) {
                return
            }

            preference.update(bubbleKey, true)
            if (bubblesQueue.isEmpty()) {
                showBubble(dest)
            }
            bubblesQueue.add(dest)
        }
    }

    private fun showBubble(dest: BubbleDest) {
        bubbleShownJob?.cancel()
        bubbleShownJob = appScope.launch {
            // Given a short delay to show bubble after UI is presented
            delay(BUBBLE_SHOWN_DELAY)
            bubble.value = dest
        }
    }

    override fun popBubble() {
        val currentBubble = bubble.value ?: return
        popBubbleJob?.cancel()
        popBubbleJob = appScope.launch {
            // Given a short delay to hide bubble with animation
            delay(BUBBLE_SHOWN_DELAY)
            bubblesQueue.remove(currentBubble)
            bubble.value = null

            if (bubblesQueue.isNotEmpty()) {
                showBubble(bubblesQueue.first())
            }
        }
    }

    private companion object {
        const val BUBBLE_SHOWN_DELAY = 200L
    }
}