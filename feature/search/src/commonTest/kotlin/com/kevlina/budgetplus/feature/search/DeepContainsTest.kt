package com.kevlina.budgetplus.feature.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeepContainsTest {

    @Test
    fun deepContains() {
        assertTrue(deepContains("1a2b3c", "123"))
        assertTrue(deepContains("abc", "ac"))
        assertTrue(deepContains("abc", "AC"))
        assertFalse(deepContains("abc", "ca"))
        assertTrue(deepContains("banana", "bna"))
        assertTrue(deepContains("apple", "aple"))
        assertFalse(deepContains("apple", "aplex"))
        assertTrue(deepContains("abc", ""))
        assertFalse(deepContains("", "a"))
    }
}
