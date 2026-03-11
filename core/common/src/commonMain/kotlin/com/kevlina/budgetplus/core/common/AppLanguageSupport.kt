package com.kevlina.budgetplus.core.common

data class SupportedAppLanguage(
    val displayName: String,
    val code: String,
    val iosPrefix: String,
)

val supportedAppLanguages = listOf(
    SupportedAppLanguage(displayName = "繁體中文", code = "zh-tw", iosPrefix = "zh-Hant"),
    SupportedAppLanguage(displayName = "简体中文", code = "zh-cn", iosPrefix = "zh-Hans"),
    SupportedAppLanguage(displayName = "日本語", code = "ja", iosPrefix = "ja"),
    SupportedAppLanguage(displayName = "한국어", code = "ko", iosPrefix = "ko"),
    SupportedAppLanguage(displayName = "English", code = "en", iosPrefix = "en"),
)

fun resolveSupportedAppLanguage(preferredLanguages: Iterable<String>): String =
    preferredLanguages.firstNotNullOfOrNull { language ->
        supportedAppLanguages.firstOrNull { language.startsWith(it.iosPrefix) }?.code
    } ?: "en"
