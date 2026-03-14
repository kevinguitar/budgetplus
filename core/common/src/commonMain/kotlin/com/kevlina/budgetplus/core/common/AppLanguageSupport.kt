package com.kevlina.budgetplus.core.common

data class SupportedAppLanguage(
    val displayName: String,
    val code: String,
)

val supportedAppLanguages = listOf(
    SupportedAppLanguage(displayName = "繁體中文", code = "zh-Hant"),
    SupportedAppLanguage(displayName = "简体中文", code = "zh-Hans"),
    SupportedAppLanguage(displayName = "日本語", code = "ja"),
    SupportedAppLanguage(displayName = "한국어", code = "ko"),
    SupportedAppLanguage(displayName = "English", code = "en"),
)

fun resolveSupportedAppLanguage(preferredLanguages: List<String>): String? =
    preferredLanguages.firstNotNullOfOrNull { language ->
        supportedAppLanguages.firstOrNull { language.startsWith(it.code) }?.code
    }
