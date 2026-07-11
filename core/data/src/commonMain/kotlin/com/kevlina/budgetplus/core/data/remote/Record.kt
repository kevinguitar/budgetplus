package com.kevlina.budgetplus.core.data.remote

import androidx.compose.runtime.Immutable
import com.kevlina.budgetplus.core.common.RecordType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.serialization.Serializable

/**
 * Represents a financial record, which can be either an expense or income.
 *
 * @param preferredPrice presented only when the record was created in the preferred currency.
 * @param preferredCurrencyCode presented only when [preferredPrice] is not null.
 */
@Immutable
@Serializable
data class Record(
    val id: String = "",
    val type: RecordType = RecordType.Expense,
    val category: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val preferredPrice: Double? = null,
    val preferredCurrencyCode: String? = null,
    val author: Author? = null,
    val date: Long = 0,
    val timestamp: Long? = null,
    val batchId: String? = null,
)

val Record.createdOn: Long
    get() = timestamp ?: LocalDate.fromEpochDays(date.toInt())
        .atStartOfDayIn(TimeZone.UTC)
        .epochSeconds

val Record.isBatched: Boolean
    get() = batchId != null
