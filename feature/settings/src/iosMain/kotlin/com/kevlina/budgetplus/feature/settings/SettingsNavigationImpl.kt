package com.kevlina.budgetplus.feature.settings

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.settings_contact_us
import budgetplus.core.common.generated.resources.settings_no_email_app_found
import com.kevlina.budgetplus.core.common.Constants.APP_LANGUAGE_INITIALIZED_KEY
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.data.AuthManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Named
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleActionSheet
import platform.UIKit.UIApplication

@ContributesBinding(AppScope::class)
class SettingsNavigationImpl(
    private val authManager: AuthManager,
    private val snackbarSender: SnackbarSender,
    @Named("contact_email") private val contactEmail: String,
) : SettingsNavigation {

    override fun openLanguageSettings(onLanguageChanged: (String) -> Unit) {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val alertController = UIAlertController.alertControllerWithTitle(
            title = null,
            message = null,
            preferredStyle = UIAlertControllerStyleActionSheet
        )

        val languages = mapOf(
            "繁體中文" to "zh-tw",
            "简体中文" to "zh-cn",
            "日本語" to "ja",
            "English" to "en"
        )

        languages.forEach { (name, code) ->
            alertController.addAction(
                UIAlertAction.actionWithTitle(
                    title = name,
                    style = UIAlertActionStyleDefault,
                    handler = {
                        val userDefaults = NSUserDefaults.standardUserDefaults
                        userDefaults.setObject(listOf(code), "AppleLanguages")
                        userDefaults.setBool(true, APP_LANGUAGE_INITIALIZED_KEY)
                        userDefaults.synchronize()
                        onLanguageChanged(code)
                    }
                )
            )
        }

        rootViewController.presentViewController(alertController, animated = true, completion = null)
    }

    override suspend fun contactUs() {
        val subject = getString(Res.string.settings_contact_us)
            .replace(" ", "%20")
        val body = "User id: ${authManager.requireUserId()}\n\n"
            .replace(" ", "%20")
            .replace("\n", "%0A")

        val mailUrl = "mailto:$contactEmail?subject=$subject&body=$body"
        val url = NSURL.URLWithString(mailUrl)

        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
        } else {
            snackbarSender.send(Res.string.settings_no_email_app_found)
        }
    }

    override fun visitUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }
}