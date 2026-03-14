package com.kevlina.budgetplus.speak.record.impl

import com.google.common.truth.Truth.assertThat
import com.kevlina.budgetplus.core.common.fixtures.FakeTracker
import com.kevlina.budgetplus.feature.speak.record.SpeakToRecordStatus
import com.kevlina.budgetplus.feature.speak.record.impl.SpeakResultParser
import org.junit.Test

class SpeakResultParserTest {

    @Test
    fun `parse normal phrase`() {
        val result = parser.parse("Bento 100")
        assertThat(result).isEqualTo(
            SpeakToRecordStatus.Success(
                name = "Bento",
                price = 100.0
            )
        )
    }

    @Test
    fun `parse normal phrase with dollar`() {
        val result = parser.parse("Bento 100 dollar")
        assertThat(result).isEqualTo(
            SpeakToRecordStatus.Success(
                name = "Bento",
                price = 100.0
            )
        )
    }

    @Test
    fun `parse normal phrase without space`() {
        val result = parser.parse("Bento100dollar")
        assertThat(result).isEqualTo(
            SpeakToRecordStatus.Success(
                name = "Bento",
                price = 100.0
            )
        )
    }

    @Test
    fun `parse price with decimal`() {
        val result = parser.parse("Supermarket 56.32")
        assertThat(result).isEqualTo(
            SpeakToRecordStatus.Success(
                name = "Supermarket",
                price = 56.32
            )
        )
    }

    @Test
    fun `parse name with number`() {
        val result = parser.parse("5 star hotel 9999")
        assertThat(result).isEqualTo(
            SpeakToRecordStatus.Success(
                name = "5 star hotel",
                price = 9999.0
            )
        )
    }

    private val parser = SpeakResultParser(
        tracker = FakeTracker()
    )
}