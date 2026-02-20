iOS integration guide for FcmIosService

This document explains how to wire iOS Firebase Cloud Messaging into the app and how to forward tokens and message payloads to the shared Kotlin `FcmIosService` (or to the existing Kotlin helper exposed by your KMP framework).

High level
- Configure Firebase in your iOS app (FirebaseApp.configure()).
- Request notification permission and register for remote notifications.
- Implement `MessagingDelegate` to receive the FCM registration token and send it to the shared Kotlin code.
- Optionally implement a `UNNotificationServiceExtension` to download rich images and attach them to notifications; forward payloads to the shared Kotlin helper for deeplink selection or other shared logic.

Prerequisites
- Add Firebase iOS SDK to your Xcode project (Swift Package Manager or CocoaPods). See: https://firebase.google.com/docs/cloud-messaging/ios/get-started
- Add `GoogleService-Info.plist` to your Xcode project.
- Enable Push Notifications and Background Modes (Remote notifications) capability.
- You already configure Firebase in `iosApp/iOSApp.swift` by calling `FirebaseApp.configure()` — keep that.

1) AppDelegate wiring (SwiftUI + UIApplicationDelegateAdaptor)

Example AppDelegate that forwards token to the shared Kotlin helper exposed in the Compose/Shared framework. The project already exposes a helper `updateFcmToken(newToken:completionHandler:)` in the generated `ComposeApp` Objective-C/Swift header — use that when available.

```swift
import UIKit
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import ComposeApp // Replace with your generated KMP framework module name

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()

        // Request permission
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            // Handle granted/error if needed
        }

        application.registerForRemoteNotifications()

        // Set Messaging delegate
        Messaging.messaging().delegate = self

        return true
    }

    // Called when FCM registration token is refreshed
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }

        // Option A (existing helper in the generated ComposeApp framework):
        // ComposeApp().updateFcmToken(newToken: token) { error in
        //     if let error = error {
        //         print("Failed to update token in Kotlin: \(error)")
        //     } else {
        //         print("Token forwarded to Kotlin: \(token)")
        //     }
        // }

        // Option B (if your app exposes a Kotlin instance of `FcmIosService` from DI):
        // Call it directly, e.g. fcmService.onNewToken(token)
        // The exact symbol name depends on how your KMP framework exposes the instance; check the generated header (.h) file for the correct class name.

        print("FCM token: \(token)")
    }

    // Optional: handle tap in-app when app is running
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        // If you attach a deeplink in userInfo.userInfo["deeplink"], navigate or forward to Kotlin
        if let deeplink = userInfo["deeplink"] as? String {
            // Handle deeplink by Swift or forward to Kotlin shared navigation
            print("Notification tapped, deeplink: \(deeplink)")
        }
        completionHandler()
    }
}
```

Notes for the AppDelegate snippet
- Replace `ComposeApp` import with the actual module name of your generated Kotlin framework if different.
- Option A is the simplest: forward the token to `ComposeApp().updateFcmToken(newToken:completionHandler:)` — this method is already present in this project's generated Objective-C/Swift header (see generated headers in the build). If that helper isn't available in your build, you can create a small top-level Kotlin function that accepts a token and forwards it to `AuthManager`.
- Option B shows the conceptual call to `FcmIosService` (the Kotlin class implemented in `core/notification/src/iosMain`). To call the methods on that class from Swift you need to expose an instance through your KMP DI graph (recommended) and check the generated header to get the exact symbol name.

2) Handling incoming messages and presenting rich notifications

- If you want to show rich images in notifications, implement a `UNNotificationServiceExtension`. In that extension you can:
  1. Inspect `request.content.userInfo` to get image URLs (keys coming from your server/payload: e.g. `smallImageUrl`, `largeImageUrl`).
  2. Download the images and attach them to the new `UNMutableNotificationContent`.
  3. Optionally call into the shared Kotlin `FcmIosService` to compute a deeplink or other logic (if exposed), or compute the deeplink in Swift using the same logic.

Example skeleton for `UNNotificationServiceExtension`:

```swift
import UserNotifications
import ComposeApp // or your generated KMP module

class NotificationService: UNNotificationServiceExtension {

    var contentHandler: ((UNNotificationContent) -> Void)?
    var bestAttemptContent: UNMutableNotificationContent?

    override func didReceive(_ request: UNNotificationRequest, with contentHandler: @escaping (UNNotificationContent) -> Void) {
        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)

        guard let bestAttemptContent = bestAttemptContent else {
            contentHandler(request.content)
            return
        }

        let userInfo = bestAttemptContent.userInfo

        // Example: compute deeplink using Kotlin helper if you have an instance
        // if let fcmService = kotlinFcmServiceInstance {
        //     let deeplink = fcmService.getDeeplinkForMessage(userInfoAsMap)
        //     bestAttemptContent.userInfo["deeplink"] = deeplink
        // }

        // Download images (if provided) and attach them
        if let imageURLString = userInfo["largeImageUrl"] as? String, let url = URL(string: imageURLString) {
            downloadAndAttach(url: url) { attachment in
                if let attachment = attachment {
                    bestAttemptContent.attachments = [attachment]
                }
                contentHandler(bestAttemptContent)
            }
        } else {
            contentHandler(bestAttemptContent)
        }
    }

    override func serviceExtensionTimeWillExpire() {
        // Called just before the extension will be terminated by the system.
        if let contentHandler = contentHandler, let bestAttemptContent = bestAttemptContent {
            contentHandler(bestAttemptContent)
        }
    }

    private func downloadAndAttach(url: URL, completion: @escaping (UNNotificationAttachment?) -> Void) {
        URLSession.shared.downloadTask(with: url) { (location, response, error) in
            var attachment: UNNotificationAttachment? = nil
            defer { completion(attachment) }
            guard let location = location else { return }
            let tmpDir = URL(fileURLWithPath: NSTemporaryDirectory())
            let uniqueURL = tmpDir.appendingPathComponent(url.lastPathComponent)
            try? FileManager.default.moveItem(at: location, to: uniqueURL)
            attachment = try? UNNotificationAttachment(identifier: "image", url: uniqueURL, options: nil)
        }.resume()
    }
}
```

3) How to call the Kotlin `FcmIosService` methods from Swift (guidance)

- If you expose an instance of the Kotlin `FcmIosService` via your DI graph (recommended), the KMP framework will generate an Objective-C/Swift compatible header that includes the class. The exact class name depends on the Kotlin package and generator, but you can find it in the generated umbrella header (search your Xcode "Headers" or the build folder under `build/bin/.../Headers`).
- Once you locate the generated Swift/ObjC name, call the methods directly, for example:
  - `fcmIosService.onNewToken(token)`
  - `fcmIosService.onMessageReceived(payloadMap)`
  - `let deeplink = fcmIosService.getDeeplinkForMessage(payloadMap)`

4) Summary & recommended flow

- Use the Swift AppDelegate / MessagingDelegate to receive a token and forward it to Kotlin using the existing `ComposeApp().updateFcmToken(...)` helper if available.
- Use a `UNNotificationServiceExtension` to download large images and attach them to notifications.
- For shared logic (deeplink selection, analytics, storing tokens), prefer forwarding payloads to your Kotlin `FcmIosService` instance exposed from DI; otherwise implement the minimal forwarding logic in Swift.

References
- Firebase iOS Messaging guide: https://firebase.google.com/docs/cloud-messaging/ios/get-started
- Apple UserNotifications: https://developer.apple.com/documentation/usernotifications

If you want, I can:
- Add a small Kotlin top-level helper that exposes simple top-level functions (e.g. `kmmUpdateFcmToken(token: String)`) so calling from Swift is straightforward, and then update this README with exact symbols to call.
- Create an example `UNNotificationServiceExtension` target with the Swift code above wired to call the Kotlin helper.

