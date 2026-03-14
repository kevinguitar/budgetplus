package com.kevlina.budgetplus.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageSupportTest {

    @Test
    fun `resolve supported app language returns korean for korean locale`() {
        assertEquals("ko", resolveSupportedAppLanguage(listOf("ko-KR")))
    }

    @Test
    fun `resolve supported app language returns japanese for japanese locale`() {
        assertEquals("ja", resolveSupportedAppLanguage(listOf("ja-JP")))
    }

    @Test
    fun `resolve supported app language returns zh-Hant for traditional chinese locale`() {
        assertEquals("zh-Hant", resolveSupportedAppLanguage(listOf("zh-Hant-TW")))
    }

    @Test
    fun `resolve supported app language returns zh-Hans for simplified chinese locale`() {
        assertEquals("zh-Hans", resolveSupportedAppLanguage(listOf("zh-Hans-CN")))
    }

    @Test
    fun `resolve supported app language returns english for english locale`() {
        assertEquals("en", resolveSupportedAppLanguage(listOf("en-US")))
    }

    @Test
    fun `resolve supported app language returns first supported language`() {
        assertEquals("ja", resolveSupportedAppLanguage(listOf("fr-FR", "ja-JP", "ko-KR")))
    }

    @Test
    fun `resolve supported app language falls back to null for unsupported languages`() {
        assertEquals(null, resolveSupportedAppLanguage(listOf("fr-FR", "de-DE")))
    }

    @Test
    fun `resolve supported app language falls back to null for empty list`() {
        assertEquals(null, resolveSupportedAppLanguage(emptyList()))
    }
}
