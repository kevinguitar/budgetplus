package com.kevlina.budgetplus.core.common.utils

import com.google.common.truth.Truth.assertThat
import com.kevlina.budgetplus.core.common.roundUpRatioText
import org.junit.Test

class NumberFormatterTest {

    @Test
    fun `WHEN a number is rounded to ratio THEN round to 1st decimal place`() {
        assertThat(23.123.roundUpRatioText).isEqualTo("23.1")
        assertThat(65.001.roundUpRatioText).isEqualTo("65")
        assertThat(45.456.roundUpRatioText).isEqualTo("45.5")
        assertThat(87.999.roundUpRatioText).isEqualTo("88")
    }
}