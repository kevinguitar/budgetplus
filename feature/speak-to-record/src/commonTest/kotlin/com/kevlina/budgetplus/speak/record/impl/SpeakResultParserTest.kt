package com.kevlina.budgetplus.speak.record.impl

import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecordStatus
import com.kevlina.budgetplus.feature.speak.record.impl.SpeakResultParser
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeakResultParserTest {

    @Test
    fun `parse normal phrase`() {
        val result = parser.parse("Bento 100")
        assertEquals(
            SpeakToRecordStatus.Success(
                name = "Bento",
                price = 100.0
            ),
            result
        )
    }

    @Test
    fun `parse normal phrase with dollar`() {
        val result = parser.parse("Bento 100 dollar")
        assertEquals(
            SpeakToRecordStatus.Success(
                name = "Bento",
                price = 100.0
            ),
            result
        )
    }

    @Test
    fun `parse normal phrase without space`() {
        val result = parser.parse("Bento100dollar")
        assertEquals(
            SpeakToRecordStatus.Success(
                name = "Bento",
                price = 100.0
            ),
            result
        )
    }

    @Test
    fun `parse price with decimal`() {
        val result = parser.parse("Supermarket 56.32")
        assertEquals(
            SpeakToRecordStatus.Success(
                name = "Supermarket",
                price = 56.32
            ),
            result
        )
    }

    @Test
    fun `parse name with number`() {
        val result = parser.parse("5 star hotel 9999")
        assertEquals(
            SpeakToRecordStatus.Success(
                name = "5 star hotel",
                price = 9999.0
            ),
            result
        )
    }

    private val parser = SpeakResultParser(
        tracker = FakeTracker()
    )
}
