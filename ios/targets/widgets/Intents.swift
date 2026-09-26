import AppIntents
import WidgetKit

/// Tap-to-complete straight from the home screen (iOS 17 interactive
/// widgets). Writes only the Done checkbox, so the sweep's "Completed on"
/// stamping keeps working exactly as with the Android widget.
struct CompleteTaskIntent: AppIntent {
    static let title: LocalizedStringResource = "Complete task"
    static let description: IntentDescription = IntentDescription("Marks a Sweep task as done in Notion.")

    @Parameter(title: "Page ID")
    var pageId: String

    @Parameter(title: "Task")
    var name: String

    init() {}

    init(pageId: String, name: String) {
        self.pageId = pageId
        self.name = name
    }

    func perform() async throws -> some IntentResult {
        try await Notion.completeTask(token: SharedStore.token, pageId: pageId, doneProp: SharedStore.doneProp)
        // Drop the row from today's cache so the immediate re-render is right
        // even if Notion's query lags the write for a moment.
        if let cache = SharedStore.loadCache() {
            SharedStore.saveCache(day: cache.day, tasks: cache.tasks.filter { $0.id != pageId })
        }
        WidgetCenter.shared.reloadTimelines(ofKind: TasksWidget.kind)
        return .result()
    }
}

struct RefreshTasksIntent: AppIntent {
    static let title: LocalizedStringResource = "Refresh tasks"
    static let description: IntentDescription = IntentDescription("Re-fetches the Sweep tasks widget from Notion.")

    init() {}

    func perform() async throws -> some IntentResult {
        WidgetCenter.shared.reloadTimelines(ofKind: TasksWidget.kind)
        return .result()
    }
}
