package com.kevlina.budgetplus.feature.settings

import budgetplus.core.common.generated.resources.Res
import budgetplus.core.common.generated.resources.cta_cancel
import budgetplus.core.common.generated.resources.settings_contact_us
import budgetplus.core.common.generated.resources.settings_no_email_app_found
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.Constants.APP_LANGUAGE_INITIALIZED_KEY
import com.kevlina.budgetplus.core.common.SnackbarSender
import com.kevlina.budgetplus.core.common.supportedAppLanguages
import com.kevlina.budgetplus.core.data.AuthManager
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleActionSheet
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

@ContributesBinding(AppScope::class)
internal class SettingsNavigationImpl(
    private val authManager: AuthManager,
    private val snackbarSender: SnackbarSender,
    @Named("contact_email") private val contactEmail: String,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : SettingsNavigation {

    override fun openLanguageSettings(onLanguageChanged: (String) -> Unit) {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val alertController = UIAlertController.alertControllerWithTitle(
            title = null,
            message = null,
            preferredStyle = UIAlertControllerStyleActionSheet
        )

        supportedAppLanguages.forEach { language ->
            alertController.addAction(
                UIAlertAction.actionWithTitle(
                    title = language.displayName,
                    style = UIAlertActionStyleDefault,
                    handler = {
                        val userDefaults = NSUserDefaults.standardUserDefaults
                        userDefaults.setObject(listOf(language.code), "AppleLanguages")
                        userDefaults.setBool(true, APP_LANGUAGE_INITIALIZED_KEY)
                        userDefaults.synchronize()
                        onLanguageChanged(language.code)
                    }
                )
            )
        }

        appScope.launch {
            alertController.addAction(
                UIAlertAction.actionWithTitle(
                    title = getString(Res.string.cta_cancel),
                    style = UIAlertActionStyleCancel,
                    handler = null
                )
            )

            alertController.popoverPresentationController?.let {
                it.sourceView = rootViewController.view
                it.sourceRect = rootViewController.view.bounds
                it.permittedArrowDirections = 0u
            }

            rootViewController.presentViewController(alertController, animated = true, completion = null)
        }
    }

    override suspend fun contactUs() {
        val subject = getString(Res.string.settings_contact_us)
        val body = "User id: ${authManager.requireUserId()}\n\n"
        val mailUrl = "mailto:$contactEmail?subject=$subject&body=$body"

        @Suppress("CAST_NEVER_SUCCEEDS")
        val encodedUrl = (mailUrl as? NSString)
            ?.stringByAddingPercentEncodingWithAllowedCharacters(NSCharacterSet.URLQueryAllowedCharacterSet)
        val url = NSURL.URLWithString(encodedUrl ?: mailUrl) ?: return

        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(
                url = url,
                options = emptyMap<Any?, Any?>(),
                completionHandler = null
            )
        } else {
            snackbarSender.send(Res.string.settings_no_email_app_found)
        }
    }

    override fun visitUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(
            url = nsUrl,
            options = emptyMap<Any?, Any?>(),
            completionHandler = null
        )
    }
}