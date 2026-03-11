package com.kevlina.budgetplus.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageSupportTest {

    @Test
    fun `resolve supported app language returns korean for korean locale`() {
        assertEquals("ko", resolveSupportedAppLanguage(listOf("ko-KR")))
    }

    @Test
    fun `resolve supported app language falls back to english`() {
        assertEquals("en", resolveSupportedAppLanguage(listOf("fr-FR")))
    }
}
