package com.kevlina.budgetplus.core.data

import com.google.common.truth.Truth.assertThat
import com.kevlina.budgetplus.core.common.plainPriceString
import org.junit.Test

class PriceFormatterTest {

    @Test
    fun `plainPriceString extension is working correctly`() {
        assertThat(12.0.plainPriceString).isEqualTo("12")
        assertThat(12.356.plainPriceString).isEqualTo("12.36")
        assertThat(12.349.plainPriceString).isEqualTo("12.35")
        assertThat(12.99.plainPriceString).isEqualTo("12.99")
        assertThat(12.999.plainPriceString).isEqualTo("13")
    }
}