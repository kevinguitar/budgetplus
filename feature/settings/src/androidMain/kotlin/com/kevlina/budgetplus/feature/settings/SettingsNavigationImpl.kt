package com.kevlina.budgetplus.feature.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.settings_contact_us
import budgetplus.core.common.generated.resources.settings_no_email_app_found
import com.kevlina.budgetplus.core.common.ActivityProvider
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.data.AuthManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Named
import org.jetbrains.compose.resources.getString

@ContributesBinding(AppScope::class)
internal class SettingsNavigationImpl(
    private val activityProvider: ActivityProvider,
    private val authManager: AuthManager,
    private val snackbarSender: SnackbarSender,
    @Named("app_package") private val appPackage: String,
    @Named("contact_email") private val contactEmail: String,
) : SettingsNavigation {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun openLanguageSettings(onLanguageChanged: (String) -> Unit) {
        val activity = activityProvider.currentActivity ?: return
        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = "package:$appPackage".toUri()
        }
        activity.startActivity(intent)
    }

    override suspend fun contactUs() {
        val activity = activityProvider.currentActivity ?: return
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(contactEmail))
            putExtra(Intent.EXTRA_SUBJECT, getString(Res.string.settings_contact_us))
            putExtra(Intent.EXTRA_TEXT, "User id: ${authManager.requireUserId()}\n\n")
        }
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        } else {
            snackbarSender.send(Res.string.settings_no_email_app_found)
        }
    }

    override fun visitUrl(url: String) {
        val activity = activityProvider.currentActivity ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
        }
        activity.startActivity(intent)
    }
}