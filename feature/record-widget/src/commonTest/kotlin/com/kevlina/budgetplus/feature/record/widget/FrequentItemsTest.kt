package com.kevlina.budgetplus.feature.record.widget

import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.data.remote.Record
import kotlin.test.Test
import kotlin.test.assertEquals

class FrequentItemsTest {

    @Test
    fun `items are grouped regardless of the price and sorted by frequency with counts`() {
        val records = listOf(
            record(category = "Food", name = "Lunch", price = 10.0),
            record(category = "Food", name = "Dinner", price = 20.0),
            record(category = "Food", name = "Dinner", price = 35.0),
            record(category = "Food", name = "Dinner ", price = 50.0),
        )

        assertEquals(
            listOf(
                FrequentItem(RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Dinner"), count = 3),
                FrequentItem(RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Lunch"), count = 1),
            ),
            records.toFrequentItems()
        )
    }

    @Test
    fun `same name of different type or category are different items`() {
        val records = listOf(
            record(category = "Food", name = "Gift"),
            record(category = "Others", name = "Gift"),
            record(category = "Others", name = "Gift", type = RecordType.Income),
        )

        assertEquals(3, records.toFrequentItems().size)
    }

    @Test
    fun `ties are broken by the most recently recorded item`() {
        val records = listOf(
            record(category = "Food", name = "Lunch", timestamp = 100),
            record(category = "Food", name = "Dinner", timestamp = 200),
        )

        assertEquals(listOf("Dinner", "Lunch"), records.toFrequentItems().map { it.item.name })
    }

    @Test
    fun `empty name falls back to the category`() {
        val records = listOf(record(category = "Food", name = ""))

        assertEquals(
            listOf(FrequentItem(RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Food"), count = 1)),
            records.toFrequentItems()
        )
    }

    @Test
    fun `items are limited`() {
        val records = (1..30).map { record(category = "Food", name = "Item $it") }

        assertEquals(FREQUENT_RECORDS_LIMIT, records.toFrequentItems().size)
    }

    @Test
    fun `pinned items come first in the given order with their counts`() {
        val records = listOf(
            record(category = "Food", name = "Dinner"),
            record(category = "Food", name = "Dinner"),
            record(category = "Food", name = "Lunch"),
            record(category = "Food", name = "Coffee"),
        )
        val lunch = RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Lunch")
        val rent = RecordWidgetItem(type = RecordType.Expense, category = "Rent", name = "Rent")

        assertEquals(
            listOf(
                FrequentItem(rent, count = 0),
                FrequentItem(lunch, count = 1),
                FrequentItem(RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Dinner"), count = 2),
                FrequentItem(RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Coffee"), count = 1),
            ),
            records.toFrequentItems(pinnedItems = listOf(rent, lunch))
        )
    }

    @Test
    fun `pinned items keep their counts beyond the limit and don't count toward it`() {
        // Item 30 is the least frequent, so it's not in the top items without pinning.
        val records = (1..30).flatMap { index ->
            List(31 - index) { record(category = "Food", name = "Item $index") }
        }
        val pinned = RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Item 30")

        val items = records.toFrequentItems(pinnedItems = listOf(pinned))

        assertEquals(FrequentItem(pinned, count = 1), items.first())
        assertEquals(FREQUENT_RECORDS_LIMIT + 1, items.size)
    }

    private fun record(
        category: String,
        name: String,
        type: RecordType = RecordType.Expense,
        price: Double = 1.0,
        timestamp: Long = 0,
    ) = Record(type = type, category = category, name = name, price = price, timestamp = timestamp)
}
