import Foundation
import SwiftUI

/// Mirrors `RecordWidgetItem` in the Kotlin `:feature:record-widget` module.
struct RecordWidgetItem: Codable, Hashable {
    let type: String
    let category: String
    let name: String

    var isIncome: Bool { type == "Income" }
}

/// Mirrors `RecordWidgetConfig` in the Kotlin `:feature:record-widget` module.
struct RecordWidgetConfig: Codable, Hashable {
    let id: String
    let bookId: String
    let bookName: String
    let items: [RecordWidgetItem]
    let createdOn: Int64
}

/// Snapshot of the app's theme colors, in hex without the leading `#`.
struct RecordWidgetTheme: Codable {
    let light: String
    let lightBg: String
    let primary: String
    let dark: String

    /// The MilkTea tone, the default theme of the app.
    static let fallback = RecordWidgetTheme(light: "FFF3E0", lightBg: "F2E2CD", primary: "C1A185", dark: "7E8072")
}

/// Reads the data written by the app to the shared App Group, see `IosRecordWidgetConfigStore.kt`.
enum RecordWidgetStore {

    private static let configsKey = "record_widget_configs"
    private static let themeKey = "record_widget_theme"

    /// The App Group is `group.<app bundle id>`, and the extension's bundle id is
    /// `<app bundle id>.RecordWidgetExtension`.
    private static var defaults: UserDefaults? {
        guard let bundleId = Bundle.main.bundleIdentifier else { return nil }
        let appBundleId = bundleId.replacingOccurrences(of: ".RecordWidgetExtension", with: "")
        return UserDefaults(suiteName: "group.\(appBundleId)")
    }

    static func configs() -> [RecordWidgetConfig] {
        guard let raw = defaults?.string(forKey: configsKey), let data = raw.data(using: .utf8) else {
            return []
        }
        return (try? JSONDecoder().decode([RecordWidgetConfig].self, from: data)) ?? []
    }

    static func config(id: String) -> RecordWidgetConfig? {
        configs().first { $0.id == id }
    }

    /// The app's localized display name, so the branding matches the name on the home screen.
    /// The extension lives in `<App>.app/PlugIns/`.
    static func appName() -> String {
        let appBundle = Bundle(url: Bundle.main.bundleURL.deletingLastPathComponent().deletingLastPathComponent())
        let name = appBundle?.localizedInfoDictionary?["CFBundleDisplayName"] as? String
            ?? appBundle?.object(forInfoDictionaryKey: "CFBundleDisplayName") as? String
        return name ?? "Budget+"
    }

    static func theme() -> RecordWidgetTheme {
        guard let raw = defaults?.string(forKey: themeKey),
              let data = raw.data(using: .utf8),
              let theme = try? JSONDecoder().decode(RecordWidgetTheme.self, from: data)
        else {
            return .fallback
        }
        return theme
    }
}

/// Mirrors `RecordWidgetDeeplink` in the Kotlin `:feature:record-widget` module.
enum RecordWidgetDeeplink {

    /// Only the RFC 3986 unreserved characters are kept as is.
    private static let unreserved = CharacterSet(
        charactersIn: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    )

    static let setup = URL(string: "budgetplus://recordWidget")!

    static func url(bookId: String, item: RecordWidgetItem) -> URL {
        let query = [
            ("source", "widget"),
            ("bookId", bookId),
            ("type", item.type),
            ("category", item.category),
            ("name", item.name),
        ]
            .map { key, value in "\(key)=\(value.addingPercentEncoding(withAllowedCharacters: unreserved) ?? "")" }
            .joined(separator: "&")
        return URL(string: "budgetplus://record?\(query)") ?? setup
    }
}

extension Color {
    init(hex: String) {
        let value = UInt64(hex, radix: 16) ?? 0
        self.init(
            red: Double((value >> 16) & 0xFF) / 255,
            green: Double((value >> 8) & 0xFF) / 255,
            blue: Double(value & 0xFF) / 255
        )
    }
}
