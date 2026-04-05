package com.kevlina.budgetplus.core.common.utils

import com.kevlina.budgetplus.core.common.roundUpRatioText
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatterTest {

    @Test
    fun `WHEN a number is rounded to ratio THEN round to 1st decimal place`() {
        assertEquals("23.1", 23.123.roundUpRatioText)
        assertEquals("65", 65.001.roundUpRatioText)
        assertEquals("45.5", 45.456.roundUpRatioText)
        assertEquals("88", 87.999.roundUpRatioText)
    }
}