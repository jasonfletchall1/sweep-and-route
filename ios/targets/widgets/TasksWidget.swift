import SwiftUI
import WidgetKit

// MARK: - Timeline

struct TasksEntry: TimelineEntry {
    let date: Date
    let tasks: [NotionTask]
    let configured: Bool
}

struct TasksProvider: TimelineProvider {
    func placeholder(in context: Context) -> TasksEntry {
        TasksEntry(
            date: Date(),
            tasks: [
                NotionTask(id: "1", name: "Call the dentist", due: nil, url: nil),
                NotionTask(id: "2", name: "Send the proposal", due: nil, url: nil),
                NotionTask(id: "3", name: "Book flights", due: nil, url: nil),
            ],
            configured: true
        )
    }

    func getSnapshot(in context: Context, completion: @escaping (TasksEntry) -> Void) {
        if context.isPreview {
            completion(placeholder(in: context))
            return
        }
        let cache = SharedStore.loadCache()
        let tasks = (cache?.day == LocalDay.iso()) ? (cache?.tasks ?? []) : []
        completion(TasksEntry(date: Date(), tasks: tasks, configured: SharedStore.tasksReady))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<TasksEntry>) -> Void) {
        Task {
            let entry = await TasksProvider.load()
            // Refresh every ~30 minutes and again right after midnight, so the
            // new day's tasks appear without a tap (WidgetKit treats this as a
            // request; the system budgets actual refreshes).
            let halfHour = Date().addingTimeInterval(30 * 60)
            let afterMidnight = LocalDay.startOfTomorrow().addingTimeInterval(60)
            completion(Timeline(entries: [entry], policy: .after(min(halfHour, afterMidnight))))
        }
    }

    /// Mirrors TasksFactory.load(): a transient failure keeps the list fetched
    /// earlier today; once the day has changed, stale rows are worse than an
    /// empty list.
    static func load() async -> TasksEntry {
        let today = LocalDay.iso()
        guard SharedStore.tasksReady else {
            return TasksEntry(date: Date(), tasks: [], configured: false)
        }
        do {
            let tasks = try await Notion.openTasks(
                token: SharedStore.token,
                databaseId: SharedStore.tasksDbId,
                doneProp: SharedStore.doneProp,
                dueProp: SharedStore.dueProp,
                dueOnOrBefore: today
            )
            SharedStore.saveCache(day: today, tasks: tasks)
            return TasksEntry(date: Date(), tasks: tasks, configured: true)
        } catch {
            if let cache = SharedStore.loadCache(), cache.day == today {
                return TasksEntry(date: Date(), tasks: cache.tasks, configured: true)
            }
            return TasksEntry(date: Date(), tasks: [], configured: true)
        }
    }
}

// MARK: - Views

struct TaskRow: View {
    let task: NotionTask

    private var due: (text: String, overdue: Bool) { DueLabel.forWidget(task.due) }

    var body: some View {
        HStack(spacing: 10) {
            Button(intent: CompleteTaskIntent(pageId: task.id, name: task.name)) {
                Image(systemName: "circle")
                    .font(.system(size: 20, weight: .regular))
                    .foregroundStyle(Palette.accent)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Mark done")

            VStack(alignment: .leading, spacing: 1) {
                Text(task.name)
                    .font(.subheadline)
                    .lineLimit(1)
                if !due.text.isEmpty {
                    Text(due.text)
                        .font(.caption2)
                        .foregroundStyle(due.overdue ? Palette.overdue : Color.secondary)
                }
            }
            Spacer(minLength: 0)
        }
    }
}

struct TasksWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: TasksEntry

    private var maxRows: Int { family == .systemLarge ? 9 : 3 }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 12) {
                Link(destination: URL(string: "sweep://tasks")!) {
                    Text("Due today")
                        .font(.headline)
                        .foregroundStyle(Palette.accent)
                }
                Spacer()
                Button(intent: RefreshTasksIntent()) {
                    Image(systemName: "arrow.clockwise")
                        .foregroundStyle(Color.secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Refresh")
                Link(destination: URL(string: "sweep://capture")!) {
                    Image(systemName: "plus")
                        .font(.headline)
                        .foregroundStyle(Palette.accent)
                }
                .accessibilityLabel("Add")
            }

            if !entry.configured {
                Text("Connect Notion in Sweep to see your tasks.")
                    .font(.subheadline)
                    .foregroundStyle(Color.secondary)
            } else if entry.tasks.isEmpty {
                Text("Nothing due — nice.")
                    .font(.subheadline)
                    .foregroundStyle(Color.secondary)
            } else {
                ForEach(entry.tasks.prefix(maxRows)) { task in
                    TaskRow(task: task)
                }
                if entry.tasks.count > maxRows {
                    Link(destination: URL(string: "sweep://tasks")!) {
                        Text("+\(entry.tasks.count - maxRows) more · All ›")
                            .font(.caption)
                            .foregroundStyle(Color.secondary)
                    }
                }
            }
            Spacer(minLength: 0)
        }
        .containerBackground(.background, for: .widget)
    }
}

// MARK: - Widget

struct TasksWidget: Widget {
    static let kind = "SweepTasks"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: TasksWidget.kind, provider: TasksProvider()) { entry in
            TasksWidgetView(entry: entry)
        }
        .configurationDisplayName("Tasks due today")
        .description("Open tasks due today or overdue, straight from Notion. Tap the circle to check one off.")
        .supportedFamilies([.systemMedium, .systemLarge])
    }
}
