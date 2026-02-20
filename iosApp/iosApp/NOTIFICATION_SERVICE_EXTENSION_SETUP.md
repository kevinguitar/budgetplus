# iOS Notification Service Extension Setup Guide

This guide explains how to set up the Notification Service Extension in Xcode for handling Firebase Cloud Messaging notifications with rich content (images) on iOS.

## What's Implemented

The Notification Service Extension (`NotificationService.swift`) provides the following functionalities:

1. **Receives Firebase Messaging Data**: Intercepts remote notifications before they're displayed
2. **Parses Image URLs**: Extracts `smallImageUrl` or `largeImageUrl` from the notification payload
3. **Renders Images**: Downloads and attaches the image to the notification (prioritizes `largeImageUrl` over `smallImageUrl`)
4. **Displays Title and Body**: Extracts and displays the `title` and `body` fields from the notification
5. **Handles Deeplinks**: Extracts the `url` field and prepares it for navigation (TODO: implement deeplink navigation properly)

## Setup Instructions in Xcode

### Step 1: Add Notification Service Extension Target

1. In Xcode, select the main project (`iosApp.xcodeproj`)
2. Go to **File → New → Target**
3. Select **Notification Service Extension**
4. Click **Next**
5. Configure the target:
   - **Product Name**: `NotificationServiceExtension`
   - **Team**: Select your development team
   - **Bundle Identifier**: `com.kevlina.budgetplus.NotificationServiceExtension` (or similar based on your main app bundle ID)
   - **Language**: Swift
6. Click **Finish**

### Step 2: Add the Extension Code

The extension files are located in:
- `iosApp/NotificationServiceExtension/NotificationService.swift`
- `iosApp/NotificationServiceExtension/Info.plist`

These should be automatically added to the new target. If not, you can manually add them through Xcode's File Inspector.

### Step 3: Verify Target Capabilities

1. Select the NotificationServiceExtension target
2. Go to **Signing & Capabilities**
3. Click **+ Capability**
4. Search for and add:
   - **Remote Notification** (if not already present)

### Step 4: Build Settings

Make sure the NotificationServiceExtension target is configured properly:

1. Select the NotificationServiceExtension target
2. Go to **Build Settings**
3. Ensure the following:
   - **iOS Deployment Target**: Matches or is lower than the main app's deployment target
   - **Swift Language Version**: Matches the main app's Swift language version

### Step 5: Update Main App Capabilities

Ensure the main `iosApp` target has **Remote Notifications** capability:

1. Select the `iosApp` target
2. Go to **Signing & Capabilities**
3. Click **+ Capability**
4. Add **Remote Notification** if not present

## How It Works

### Notification Payload Format

Send notifications from Firebase Cloud Messaging with the following data structure:

```json
{
  "title": "Notification Title",
  "body": "Notification Body",
  "url": "deeplink://path/to/destination",
  "smallImageUrl": "https://example.com/small-image.png",
  "largeImageUrl": "https://example.com/large-image.png"
}
```

### Processing Flow

1. **NotificationService Extension** intercepts the notification
2. Extracts `title`, `body`, `url`, and image URLs from the payload
3. Downloads the image (prioritizes `largeImageUrl`)
4. Attaches the image to the notification content
5. Displays the enhanced notification to the user

### Deeplink Handling

When a user taps a notification, the `userNotificationCenter(_:didReceive:withCompletionHandler:)` method in `AppDelegate` is called. The deeplink can be extracted from the `userInfo` dictionary and used to navigate the app.

**TODO**: Implement proper deeplink navigation by connecting it to your app's navigation system or shared Kotlin code.

## Testing

To test the Notification Service Extension:

1. Build and run the app on a physical iOS device (extensions don't work on simulators)
2. Send a test notification using Firebase Console with the data format described above
3. The notification should be delivered with the image attached

## Troubleshooting

- **Extension not running**: Ensure it's included in the app target's scheme (Product → Scheme → Edit Scheme)
- **Image not loading**: Check network connectivity and verify the image URL is accessible
- **Extension crashes**: Check the Console logs for errors and review the `didReceive` method implementation

## References

- [Firebase Cloud Messaging - iOS Setup](https://firebase.google.com/docs/cloud-messaging/ios/get-started)
- [Rich Notifications - Apple Documentation](https://developer.apple.com/documentation/usernotifications/rich_notifications)

