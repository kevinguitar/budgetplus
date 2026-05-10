package com.kevlina.budgetplus.core.common

import android.content.Intent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@ContributesBinding(AppScope::class)
internal class ShareHelperImpl(
    private val activity: ActivityProvider,
) : ShareHelper {

    override suspend fun share(title: StringResource, text: String) {
        val activity = activity.currentActivity ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        activity.startActivity(Intent.createChooser(intent, getString(title)))
    }
}