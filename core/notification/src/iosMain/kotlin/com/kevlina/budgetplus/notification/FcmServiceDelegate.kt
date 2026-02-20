package com.kevlina.budgetplus.notification

import co.touchlab.kermit.Logger
import com.kevlina.budgetplus.core.common.AppCoroutineScope
import com.kevlina.budgetplus.core.common.nav.APP_DEEPLINK
import com.kevlina.budgetplus.core.common.nav.BookDest
import com.kevlina.budgetplus.core.common.nav.NAV_SETTINGS_PATH
import com.kevlina.budgetplus.core.common.nav.NavController
import com.kevlina.budgetplus.notification.NotificationConstants.CHANNEL_GENERAL
import com.kevlina.budgetplus.notification.NotificationConstants.CHANNEL_NEW_MEMBER
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// A lightweight iOS-side counterpart to the Android `FcmService`.
// This class is intended to be called from the iOS AppDelegate/Swift code that integrates
// with Firebase iOS SDK (see https://firebase.google.com/docs/cloud-messaging/ios/get-started).
// The iOS app should forward token updates and remote messages to this class.

@Inject
class FcmServiceDelegate(
//    private val imageLoader: ImageLoader,
    @Named("default_deeplink") private val defaultDeeplink: String,
    @AppCoroutineScope private val appScope: CoroutineScope,
    private val navController: NavController<BookDest>,
) {
    // Called by iOS AppDelegate when a remote message is received while the app is in foreground
    // The `data` map contains the message payload (equivalent to RemoteMessage.data on Android)
    fun onMessageReceived(data: Map<String, String>) {
        Logger.d { "iOS RemoteMessage: $data" }

        val channelId = when (data["type"]) {
            CHANNEL_NEW_MEMBER -> CHANNEL_NEW_MEMBER
            CHANNEL_GENERAL -> CHANNEL_GENERAL
            else -> CHANNEL_GENERAL
        }

        // Compose the deeplink/url to open if user taps the notification. Actual iOS notification
        // presentation and tap handling should be implemented on the Swift side. Here we return
        // the chosen deeplink for Swift to attach to the UNNotificationContent's userInfo if desired.
        val contentUrl = if (channelId == CHANNEL_NEW_MEMBER) {
            "${APP_DEEPLINK}/${NAV_SETTINGS_PATH}?showMembers=true"
        } else {
            data["url"] ?: defaultDeeplink
        }

        // Load images asynchronously – Swift/UNNotificationServiceExtension may handle this better.
        appScope.launch(Dispatchers.IO) {
//            val smallImage = imageLoader.execute(ImageRequest.Builder(PlatformContext.INSTANCE).data(data["smallImageUrl"]).build())
//            val largeImage = imageLoader.execute(ImageRequest.Builder(PlatformContext.INSTANCE).data(data["largeImageUrl"]).build())

            withContext(Dispatchers.Main) {
                // We can't directly post Android style Notification on iOS. Instead, provide a
                // small, generic log and expose helper methods/data for Swift to create
                // a UNNotificationContent/UNNotificationRequest when needed.
                Logger.d { "iOS: Prepared notification assets for url=$contentUrl" }
            }
        }
    }
}

