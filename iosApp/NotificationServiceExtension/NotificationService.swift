import UserNotifications
import UIKit
import ComposeApp

// Not nice duplication but it's the easy way for now
private let APP_DEEPLINK = "https://budgetplus.cchi.tw"
private let NAV_SETTINGS_PATH = "settings"

class NotificationService: UNNotificationServiceExtension {

    var contentHandler: ((UNNotificationContent) -> Void)?
    var bestAttemptContent: UNMutableNotificationContent?

    override func didReceive(_ request: UNNotificationRequest, withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void) {
        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)

        guard let bestAttemptContent = bestAttemptContent else {
            contentHandler(request.content)
            return
        }

        // Extract data from the notification payload
        let userInfo = request.content.userInfo

        // Set title and body from the notification data
        if let title = userInfo["title"] as? String {
            bestAttemptContent.title = title
        }

        if let body = userInfo["body"] as? String {
            bestAttemptContent.body = body
        }

        // Handle url deeplink based on notification type
        let type = userInfo["type"] as? String
        let url: String

        if type == "new_member" {
            url = "\(APP_DEEPLINK)/\(NAV_SETTINGS_PATH)?showMembers=true"
        } else {
            url = (userInfo["url"] as? String) ?? BudgetPlusIosAppGraphHolder.shared.graph.defaultDeeplink
        }

        bestAttemptContent.userInfo["deeplink"] = url

        // Try to load and attach the image
        loadAndAttachImage(
            smallImageUrl: userInfo["smallImageUrl"] as? String,
            largeImageUrl: userInfo["largeImageUrl"] as? String,
            to: bestAttemptContent
        )

        contentHandler(bestAttemptContent)
    }

    override func serviceExtensionTimeWillExpire() {
        // Called just before the extension will be terminated by the system.
        // Use this as an opportunity to deliver your "best attempt" at modified content,
        // otherwise the original notification will be used.
        if let contentHandler = contentHandler, let bestAttemptContent = bestAttemptContent {
            contentHandler(bestAttemptContent)
        }
    }

    // MARK: - Private Methods

    private func loadAndAttachImage(
        smallImageUrl: String?,
        largeImageUrl: String?,
        to content: UNMutableNotificationContent
    ) {
        // Prioritize largeImageUrl over smallImageUrl
        let imageUrlString = largeImageUrl ?? smallImageUrl
        guard let imageUrlString = imageUrlString else {
            return
        }

        guard let imageUrl = URL(string: imageUrlString) else {
            return
        }

        do {
            let imageData = try Data(contentsOf: imageUrl)
            guard let image = UIImage(data: imageData) else {
                return
            }

            // Create a temporary file to store the image
            let fileName = UUID().uuidString + ".png"
            let temporaryDirectory = FileManager.default.temporaryDirectory
            let fileUrl = temporaryDirectory.appendingPathComponent(fileName)

            // Write image to file
            try image.pngData()?.write(to: fileUrl)

            // Create an attachment and add it to the notification content
            let attachment = try UNNotificationAttachment(identifier: "image", url: fileUrl, options: nil)
            content.attachments = [attachment]
        } catch {
            // Silently fail and display notification without image
            print("Failed to load or attach image: \(error)")
        }
    }
}
