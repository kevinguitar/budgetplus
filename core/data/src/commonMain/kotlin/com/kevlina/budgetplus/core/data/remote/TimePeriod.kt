package com.kevlina.budgetplus.core.data.remote

import androidx.compose.runtime.Immutable
import com.kevlina.budgetplus.core.common.now
import com.kevlina.budgetplus.core.common.shortFormatted
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

@Immutable
@Serializable
sealed class TimePeriod {

    abstract val from: LocalDate
    abstract val until: LocalDate

    @Serializable
    data object Today : TimePeriod() {

        private val now get() = LocalDate.now()

        override val from: LocalDate get() = now
        override val until: LocalDate get() = now
    }

    @Serializable
    data object Week : TimePeriod() {

        private val now get() = LocalDate.now()
        private val dayOfWeek get() = now.dayOfWeek.isoDayNumber
        private const val WEEK_DAYS = 7

        override val from: LocalDate get() = now.minus(dayOfWeek - 1, DateTimeUnit.DAY)
        override val until: LocalDate get() = now.plus(WEEK_DAYS - dayOfWeek, DateTimeUnit.DAY)
    }

    @Serializable
    data object Month : TimePeriod() {

        private val now get() = LocalDate.now()

        override val from: LocalDate get() = LocalDate(now.year, now.month, 1)
        override val until: LocalDate
            get() = from
                .plus(1, DateTimeUnit.MONTH)
                .minus(1, DateTimeUnit.DAY)
    }

    @Serializable
    data object LastMonth : TimePeriod() {

        private val now get() = LocalDate.now()
        private val lastMonthDate get() = now.minus(1, DateTimeUnit.MONTH)

        override val from: LocalDate
            get() = LocalDate(lastMonthDate.year, lastMonthDate.month, 1)

        override val until: LocalDate
            get() = from
                .plus(1, DateTimeUnit.MONTH)
                .minus(1, DateTimeUnit.DAY)
    }

    @Serializable
    data class Custom(
        override val from: LocalDate,
        override val until: LocalDate,
    ) : TimePeriod() {

        init {
            check(from <= until) {
                "From date is later than until."
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TimePeriod) return false

            return from == other.from && until == other.until
        }

        override fun hashCode(): Int {
            var result = from.hashCode()
            result = 31 * result + until.hashCode()
            return result
        }
    }

    override fun toString(): String {
        return "TimePeriod from ${from.shortFormatted} to ${until.shortFormatted}"
    }
}