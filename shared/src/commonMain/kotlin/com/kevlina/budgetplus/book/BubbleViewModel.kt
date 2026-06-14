package com.kevlina.budgetplus.book

import androidx.lifecycle.ViewModel
import com.kevlina.budgetplus.core.ui.bubble.BubbleRepo
import dev.zacsweers.metro.Inject

@Inject
class BubbleViewModel(
    private val bubbleRepo: BubbleRepo,
) : ViewModel() {

    val destination = bubbleRepo.bubble

    fun dismissBubble() {
        bubbleRepo.popBubble()
    }
}