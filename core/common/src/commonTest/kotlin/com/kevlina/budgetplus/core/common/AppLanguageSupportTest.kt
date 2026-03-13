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
    fun `resolve supported app language returns zh-tw for traditional chinese locale`() {
        assertEquals("zh-tw", resolveSupportedAppLanguage(listOf("zh-Hant-TW")))
    }

    @Test
    fun `resolve supported app language returns zh-cn for simplified chinese locale`() {
        assertEquals("zh-cn", resolveSupportedAppLanguage(listOf("zh-Hans-CN")))
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
    fun `resolve supported app language falls back to english for unsupported languages`() {
        assertEquals("en", resolveSupportedAppLanguage(listOf("fr-FR", "de-DE")))
    }

    @Test
    fun `resolve supported app language falls back to english for empty list`() {
        assertEquals("en", resolveSupportedAppLanguage(emptyList()))
    }
}
