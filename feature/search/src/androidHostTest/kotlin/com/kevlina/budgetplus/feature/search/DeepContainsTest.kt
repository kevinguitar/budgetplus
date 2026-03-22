package com.kevlina.budgetplus.feature.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeepContainsTest {

    @Test
    fun deepContains() {
        assertThat(deepContains("1a2b3c", "123")).isTrue()
        assertThat(deepContains("abc", "ac")).isTrue()
        assertThat(deepContains("abc", "AC")).isTrue()
        assertThat(deepContains("abc", "ca")).isFalse()
        assertThat(deepContains("banana", "bna")).isTrue()
        assertThat(deepContains("apple", "aple")).isTrue()
        assertThat(deepContains("apple", "aplex")).isFalse()
        assertThat(deepContains("abc", "")).isTrue()
        assertThat(deepContains("", "a")).isFalse()
    }
}