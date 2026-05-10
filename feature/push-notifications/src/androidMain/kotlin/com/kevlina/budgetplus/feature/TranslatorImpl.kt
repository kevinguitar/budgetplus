package com.kevlina.budgetplus.feature

import co.touchlab.kermit.Logger
import com.google.auth.ApiKeyCredentials
import com.google.cloud.translate.Translate.TranslateOption
import com.google.cloud.translate.TranslateOptions
import com.kevlina.budgetplus.feature.push.notifications.Translator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@ContributesBinding(AppScope::class)
internal class TranslatorImpl(
    @Named("google_api_key") googleApiKey: String,
) : Translator {

    private val translationService by lazy {
        TranslateOptions.newBuilder()
            .setCredentials(ApiKeyCredentials.create(googleApiKey))
            .build()
            .service
    }

    override suspend fun translate(
        text: String,
        sourceLanCode: String?,
        targetLanCode: String,
    ): String = withContext(Dispatchers.IO) {
        try {
            translationService.translate(
                text,
                TranslateOption.sourceLanguage(sourceLanCode),
                TranslateOption.targetLanguage(targetLanCode)
            ).translatedText
        } catch (e: Exception) {
            Logger.e(e) { "Translation failed, return original text as fallback." }
            text
        }
    }
}