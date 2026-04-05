package com.kevlina.budgetplus.core.data

import com.kevlina.budgetplus.core.common.plainPriceString
import kotlin.test.Test
import kotlin.test.assertEquals

class PriceFormatterTest {

    @Test
    fun `plainPriceString extension is working correctly`() {
        assertEquals("12", 12.0.plainPriceString)
        assertEquals("12.36", 12.356.plainPriceString)
        assertEquals("12.35", 12.349.plainPriceString)
        assertEquals("12.99", 12.99.plainPriceString)
        assertEquals("13", 12.999.plainPriceString)
    }
}