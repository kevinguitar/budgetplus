import AppIntents
import WidgetKit

/// A preset (book + items) saved from the app's "Record Widget" screen.
struct RecordWidgetPresetEntity: AppEntity {

    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Preset"
    static var defaultQuery = RecordWidgetPresetQuery()

    let id: String
    let bookName: String
    let itemNames: [String]

    init(config: RecordWidgetConfig) {
        id = config.id
        bookName = config.bookName
        itemNames = config.items.map(\.name)
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(bookName)",
            subtitle: "\(itemNames.joined(separator: ", "))"
        )
    }
}

struct RecordWidgetPresetQuery: EntityQuery {

    func entities(for identifiers: [RecordWidgetPresetEntity.ID]) async throws -> [RecordWidgetPresetEntity] {
        RecordWidgetStore.configs()
            .filter { identifiers.contains($0.id) }
            .map(RecordWidgetPresetEntity.init)
    }

    func suggestedEntities() async throws -> [RecordWidgetPresetEntity] {
        RecordWidgetStore.configs()
            .sorted { $0.createdOn > $1.createdOn }
            .map(RecordWidgetPresetEntity.init)
    }

    /// The most recently saved preset is selected by default.
    func defaultResult() async -> RecordWidgetPresetEntity? {
        try? await suggestedEntities().first
    }
}

struct RecordWidgetConfigurationIntent: WidgetConfigurationIntent {

    static var title: LocalizedStringResource = "Record Widget"
    static var description = IntentDescription("Record your frequent expenses with a tap")

    @Parameter(title: "Preset")
    var preset: RecordWidgetPresetEntity?
}
