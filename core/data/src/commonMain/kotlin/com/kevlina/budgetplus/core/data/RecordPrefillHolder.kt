package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.common.RecordType
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * The record info to be filled in the record screen, e.g. when the user taps on an item from
 * the record widget.
 *
 * @param bookId The book the record is meant for, the record screen applies the prefill only
 *  after this book becomes the current book.
 */
data class RecordPrefill(
    val bookId: String,
    val type: RecordType,
    val category: String,
    val name: String,
)

/**
 * Hands off a [RecordPrefill] from the deeplink handler to the record screen.
 */
@SingleIn(AppScope::class)
@Inject
class RecordPrefillHolder {

    val prefill: StateFlow<RecordPrefill?>
        field = MutableStateFlow<RecordPrefill?>(null)

    fun set(prefill: RecordPrefill) {
        this.prefill.value = prefill
    }

    /**
     * @return The pending prefill and clear it, so it's applied only once.
     */
    fun consume(): RecordPrefill? = prefill.getAndUpdate { null }
}
