import SwiftUI
import WidgetKit

struct RecordWidgetEntry: TimelineEntry {
    let date: Date
    let config: RecordWidgetConfig?
    let theme: RecordWidgetTheme
}

struct RecordWidgetProvider: AppIntentTimelineProvider {

    func placeholder(in context: Context) -> RecordWidgetEntry {
        RecordWidgetEntry(date: .now, config: .placeholder, theme: .fallback)
    }

    func snapshot(for configuration: RecordWidgetConfigurationIntent, in context: Context) async -> RecordWidgetEntry {
        let entry = await entry(for: configuration)
        // Show a sample in the widget gallery before any preset is saved.
        if context.isPreview && entry.config == nil {
            return placeholder(in: context)
        }
        return entry
    }

    func timeline(for configuration: RecordWidgetConfigurationIntent, in context: Context) async -> Timeline<RecordWidgetEntry> {
        // The app reloads the timelines whenever the presets or the theme change.
        Timeline(entries: [await entry(for: configuration)], policy: .never)
    }

    private func entry(for configuration: RecordWidgetConfigurationIntent) async -> RecordWidgetEntry {
        let presetId: String?
        if let preset = configuration.preset {
            presetId = preset.id
        } else {
            presetId = await RecordWidgetPresetQuery().defaultResult()?.id
        }
        return RecordWidgetEntry(
            date: .now,
            config: presetId.flatMap(RecordWidgetStore.config(id:)),
            theme: RecordWidgetStore.theme()
        )
    }
}

struct RecordWidget: Widget {

    let kind = "RecordWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: RecordWidgetConfigurationIntent.self,
            provider: RecordWidgetProvider()
        ) { entry in
            RecordWidgetView(entry: entry)
                .containerBackground(Color(hex: entry.theme.light), for: .widget)
        }
        .configurationDisplayName("Record Widget")
        .description("Record your frequent expenses with a tap")
        .supportedFamilies([.systemMedium, .systemLarge])
        .contentMarginsDisabled()
    }
}

struct RecordWidgetView: View {

    @Environment(\.widgetFamily) private var family

    let entry: RecordWidgetEntry

    /// iOS widgets can't scroll, so the medium widget shows fewer items.
    private var maxItems: Int { family == .systemLarge ? 5 : 3 }

    var body: some View {
        if let config = entry.config {
            content(config: config)
        } else {
            Link(destination: RecordWidgetDeeplink.setup) {
                Text("Set up in \(RecordWidgetStore.appName())")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(Color(hex: entry.theme.dark))
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
    }

    private func content(config: RecordWidgetConfig) -> some View {
        let items = Array(config.items.prefix(maxItems))
        let hiddenCount = config.items.count - items.count

        return VStack(alignment: .leading, spacing: 0) {
            HStack {
                Text(config.bookName)
                    .font(.system(size: 16, weight: .bold))
                    .lineLimit(1)
                Spacer()
                if hiddenCount > 0 {
                    Text("+\(hiddenCount)")
                        .font(.system(size: 13, weight: .medium))
                }
            }
            .foregroundStyle(Color(hex: entry.theme.light))
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color(hex: entry.theme.dark))

            ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                Link(destination: RecordWidgetDeeplink.url(bookId: config.bookId, item: item)) {
                    itemRow(item: item, isLast: index == items.count - 1)
                }
            }

        }
        // Keep the book name at the top, the items which don't fit are cropped at the bottom.
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .clipped()
    }

    private func itemRow(item: RecordWidgetItem, isLast: Bool) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            // Show the name and the pills in the same row when there is enough room, otherwise
            // move the pills to the 2nd row.
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 8) {
                    itemName(item.name)
                        .fixedSize()
                    pills(item: item)
                        .fixedSize()
                }
                .padding(.vertical, 4)

                VStack(alignment: .leading, spacing: 4) {
                    itemName(item.name)
                    pills(item: item)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.vertical, 6)

            if !isLast {
                Rectangle()
                    .fill(Color(hex: entry.theme.lightBg))
                    .frame(height: 1)
            }
        }
    }

    private func itemName(_ name: String) -> some View {
        Text(name)
            .font(.system(size: 15, weight: .medium))
            .foregroundStyle(Color(hex: entry.theme.dark))
            .lineLimit(1)
    }

    private func pills(item: RecordWidgetItem) -> some View {
        HStack(spacing: 6) {
            pill(text: Text(item.category))
            if item.isIncome {
                pill(text: Text("Income"))
            }
        }
    }

    private func pill(text: Text) -> some View {
        text
            .font(.system(size: 12))
            .lineLimit(1)
            .foregroundStyle(Color(hex: entry.theme.light))
            .padding(.horizontal, 8)
            .padding(.vertical, 1)
            .background(Color(hex: entry.theme.primary), in: Capsule())
    }
}

private extension RecordWidgetConfig {
    static let placeholder = RecordWidgetConfig(
        id: "placeholder",
        bookId: "",
        bookName: "Budget+",
        items: [
            RecordWidgetItem(type: "Expense", category: "Food", name: "Dinner"),
            RecordWidgetItem(type: "Expense", category: "Transport", name: "Transport"),
            RecordWidgetItem(type: "Expense", category: "Food", name: "Coffee"),
        ],
        createdOn: 0
    )
}
