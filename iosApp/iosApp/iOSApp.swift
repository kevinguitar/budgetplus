import Combine
import ComposeApp
import FirebaseCore
import FirebaseFirestore
import FirebaseMessaging
import SwiftUI

// DeeplinkManager to handle deeplink communication between AppDelegate and SwiftUI
class DeeplinkManager: NSObject, ObservableObject {
    @Published var deeplink: String?
    static let shared = DeeplinkManager()
}

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

        DeeplinkManager.shared.deeplink = incomingURL.absoluteString
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        if let deeplink = userInfo["deeplink"] as? String {
            DeeplinkManager.shared.deeplink = deeplink
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
    @StateObject private var deeplinkManager = DeeplinkManager.shared

    var body: some Scene {
        WindowGroup {
            ContentView(deeplink: deeplinkManager.deeplink)
                .onOpenURL { url in
                    deeplinkManager.deeplink = url.absoluteString
                }
        }
    }
}
