package com.kevlina.budgetplus.feature.overview

import com.kevlina.budgetplus.core.common.RecordType
import com.kevlina.budgetplus.core.common.getCurrencySymbol
import com.kevlina.budgetplus.core.data.remote.Record
import com.kevlina.budgetplus.core.unit.test.BaseTest
import com.kevlina.budgetplus.feature.overview.utils.recordedPriceOrEmpty
import kotlin.test.Test
import kotlin.test.assertEquals

class CsvExporterTest : BaseTest() {

    @Test
    fun `WHEN preferredPrice is present THEN formats it with the currency symbol`() {
        val record = Record(
            type = RecordType.Expense,
            price = 3000.0,
            preferredPrice = 100.0,
            preferredCurrencyCode = "JPY",
        )
        assertEquals("100 ${getCurrencySymbol("JPY")}", record.recordedPriceOrEmpty())
    }

    @Test
    fun `WHEN preferredPrice is null THEN returns an empty string`() {
        val record = Record(
            type = RecordType.Expense,
            price = 150.0,
            preferredPrice = null,
        )
        assertEquals("", record.recordedPriceOrEmpty())
    }

    @Test
    fun `WHEN preferredPrice has decimals THEN formats it as a plain price`() {
        val record = Record(
            price = 12.0,
            preferredPrice = 12.356,
            preferredCurrencyCode = "USD",
        )
        assertEquals("12.36 ${getCurrencySymbol("USD")}", record.recordedPriceOrEmpty())
    }

    @Test
    fun `WHEN preferredPrice is a whole number THEN omits the decimals`() {
        val record = Record(
            price = 12.0,
            preferredPrice = 12.0,
            preferredCurrencyCode = "USD",
        )
        assertEquals("12 ${getCurrencySymbol("USD")}", record.recordedPriceOrEmpty())
    }

    @Test
    fun `WHEN preferredCurrencyCode is null THEN falls back to the default currency symbol`() {
        val record = Record(
            price = 12.0,
            preferredPrice = 50.0,
            preferredCurrencyCode = null,
        )
        assertEquals("50 ${getCurrencySymbol(null)}", record.recordedPriceOrEmpty())
    }
}
