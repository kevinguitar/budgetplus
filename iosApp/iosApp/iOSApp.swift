import Combine
import ComposeApp
import FirebaseCore
import FirebaseFirestore
import FirebaseMessaging
import SwiftUI

// For full explanation
// https://firebase.google.com/docs/ios/learn-more?hl=en#swiftui
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        Messaging.messaging().delegate = self

        UNUserNotificationCenter.current().delegate = self

        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(
            options: authOptions,
            completionHandler: { _, _ in }
        )

        application.registerForRemoteNotifications()

        BudgetPlusIosAppGraphHolder.shared.graph.appStartActions.forEach { action in
            (action as? CommonAppStartAction)?.onAppStart()
        }

        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(
        _ application: UIApplication,
        continue userActivity: NSUserActivity,
        restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void
    ) -> Bool {
        guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
            let incomingURL = userActivity.webpageURL
        else {
            return false
        }

        BudgetPlusIosAppGraphHolder.shared.onNewDeeplink(deeplink: incomingURL.absoluteString)
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        if let deeplink = userInfo["deeplink"] as? String {
            print("Received deeplink from notification: \(deeplink)")
            BudgetPlusIosAppGraphHolder.shared.onNewDeeplink(deeplink: deeplink)
        }
        completionHandler()
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        if let fcmToken = fcmToken {
            BudgetPlusIosAppGraphHolder.shared.graph.authManager.updateFcmToken(newToken: fcmToken)
        }
    }
}

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    BudgetPlusIosAppGraphHolder.shared.onNewDeeplink(deeplink: url.absoluteString)
                }
        }
    }
}
