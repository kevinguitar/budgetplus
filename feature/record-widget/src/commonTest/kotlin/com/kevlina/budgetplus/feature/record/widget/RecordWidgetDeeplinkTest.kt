package com.kevlina.budgetplus.feature.record.widget

import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.data.RecordPrefill
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecordWidgetDeeplinkTest {

    @Test
    fun `build should encode the item into a record deeplink`() {
        val link = RecordWidgetDeeplink.build(
            bookId = "book",
            item = RecordWidgetItem(type = RecordType.Expense, category = "Food", name = "Dinner & Drinks"),
        )

        assertEquals(
            "budgetplus://record?source=widget&bookId=book&type=Expense&category=Food&name=Dinner%20%26%20Drinks",
            link
        )
    }

    @Test
    fun `parse should decode the item built by build`() {
        val item = RecordWidgetItem(type = RecordType.Income, category = "薪水 💰", name = "C++ = 100%")
        val link = RecordWidgetDeeplink.build(bookId = "book", item = item)

        assertEquals(
            RecordWidgetLink.Item(
                RecordPrefill(bookId = "book", type = RecordType.Income, category = "薪水 💰", name = "C++ = 100%")
            ),
            RecordWidgetDeeplink.parse(link.toQueries())
        )
    }

    @Test
    fun `parse should fallback the name to category`() {
        val link = RecordWidgetDeeplink.parse(
            mapOf("source" to "widget", "bookId" to "book", "type" to "Expense", "category" to "Food")
        )

        assertEquals("Food", (link as? RecordWidgetLink.Item)?.prefill?.name)
    }

    @Test
    fun `parse should return null when the link isn't from the widget`() {
        assertNull(RecordWidgetDeeplink.parse(mapOf("bookId" to "book", "type" to "Expense", "category" to "Food")))
    }

    @Test
    fun `parse should return null when required fields are missing or invalid`() {
        val valid = mapOf("source" to "widget", "bookId" to "book", "type" to "Expense", "category" to "Food")

        assertNull(RecordWidgetDeeplink.parse(valid - "bookId"))
        assertNull(RecordWidgetDeeplink.parse(valid - "category"))
        assertNull(RecordWidgetDeeplink.parse(valid + ("type" to "Transfer")))
    }

    @Test
    fun `buildBookLink should only contain the source and the book`() {
        assertEquals("budgetplus://record?source=widget&bookId=my%20book", RecordWidgetDeeplink.buildBookLink("my book"))
    }

    @Test
    fun `parse should decode the book link built by buildBookLink`() {
        val link = RecordWidgetDeeplink.buildBookLink(bookId = "家庭 & 旅行")

        assertEquals(RecordWidgetLink.OpenBook(bookId = "家庭 & 旅行"), RecordWidgetDeeplink.parse(link.toQueries()))
    }

    @Test
    fun `parse should never treat a malformed item link as a book link`() {
        val bookLink = mapOf("source" to "widget", "bookId" to "book")

        assertNull(RecordWidgetDeeplink.parse(bookLink + ("type" to "Expense")))
        assertNull(RecordWidgetDeeplink.parse(bookLink + ("category" to "Food")))
        assertNull(RecordWidgetDeeplink.parse(bookLink + ("name" to "Dinner")))
        assertNull(RecordWidgetDeeplink.parse(bookLink - "bookId"))
    }

    @Test
    fun `percentDecode should keep malformed escapes as literal text`() {
        assertEquals("100%", "100%".percentDecode())
        assertEquals("%zz", "%zz".percentDecode())
        assertEquals("a+b", "a+b".percentDecode())
    }

    private fun String.toQueries(): Map<String, String> = substringAfter("?")
        .split("&")
        .associate { it.substringBefore("=") to it.substringAfter("=") }
}
