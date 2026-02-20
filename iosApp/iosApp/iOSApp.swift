import Combine
import ComposeApp
import FirebaseCore
import FirebaseFirestore
import SwiftUI

// DeeplinkManager to handle deeplink communication between AppDelegate and SwiftUI
class DeeplinkManager: NSObject, ObservableObject {
    @Published var deeplink: String?
    static let shared = DeeplinkManager()
}

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
        if let deeplink = userInfo["deeplink"] as? String {
            DeeplinkManager.shared.deeplink = deeplink
        }
        completionHandler()
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
