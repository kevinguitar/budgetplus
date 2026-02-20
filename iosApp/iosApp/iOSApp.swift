import ComposeApp
import FirebaseCore
import FirebaseFirestore
import SwiftUI

// For full explanation
// https://firebase.google.com/docs/ios/learn-more?hl=en#swiftui
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()

        UNUserNotificationCenter.current().delegate = self

        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(
            options: authOptions,
            completionHandler: { _, _ in }
        )

        application.registerForRemoteNotifications()

        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        if let deeplink = userInfo["url"] as? String {
            // Handle deeplink by Swift or forward to Kotlin shared navigation
            // BudgetPlusIosAppGraphHolder.shared.graph.fcmServiceDelegate.onNotificationTapped(deeplink: deeplink)
            print("Notification tapped, deeplink: \(deeplink)")
        }
        completionHandler()
    }
}

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @State private var deeplink: String?

    var body: some Scene {
        WindowGroup {
            ContentView(deeplink: deeplink)
                .onOpenURL { url in
                    deeplink = url.absoluteString
                }
        }
    }
}
