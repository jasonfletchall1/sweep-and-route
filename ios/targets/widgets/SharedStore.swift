import Foundation
import SwiftUI

/// Reads the configuration the app writes into the App Group's UserDefaults
/// (see modules/sweep-shared/ios/SweepSharedModule.swift). Keys match the
/// Android app's SharedPreferences keys one for one.
enum SharedStore {
    static let suiteName = "group.us.northlafayette.sweep"

    static var defaults: UserDefaults? { UserDefaults(suiteName: suiteName) }

    static func string(_ key: String, default fallback: String = "") -> String {
        let value = defaults?.string(forKey: key) ?? ""
        return value.isEmpty ? fallback : value
    }

    static var token: String { string("token") }
    static var inboxDbId: String { string("inboxDbId") }
    static var tasksDbId: String { string("tasksDbId") }
    static var doneProp: String { string("doneProp", default: "Done") }
    static var dueProp: String { string("dueProp", default: "Due") }

    static var tasksReady: Bool { !token.isEmpty && !tasksDbId.isEmpty }

    // MARK: - Cache of the last successful fetch

    /// The local calendar day the cached list was fetched for. Anything served
    /// from the cache is checked against today's date first, so the widget
    /// never keeps showing yesterday's list (the Android day-rollover bug).
    private static let cacheKey = "widgetTasksCache"
    private static let cacheDayKey = "widgetTasksCacheDay"

    struct Cache {
        let day: String
        let tasks: [NotionTask]
    }

    static func loadCache() -> Cache? {
        guard let day = defaults?.string(forKey: cacheDayKey),
              let data = defaults?.data(forKey: cacheKey),
              let tasks = try? JSONDecoder().decode([NotionTask].self, from: data)
        else { return nil }
        return Cache(day: day, tasks: tasks)
    }

    static func saveCache(day: String, tasks: [NotionTask]) {
        guard let data = try? JSONEncoder().encode(tasks) else { return }
        defaults?.set(data, forKey: cacheKey)
        defaults?.set(day, forKey: cacheDayKey)
    }
}

/// Local-calendar-day helpers. "Due today" is defined by the phone's own
/// calendar day, exactly like LocalDate.now() on Android.
enum LocalDay {
    private static func formatter(_ format: String) -> DateFormatter {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.calendar = Calendar.current
        f.timeZone = TimeZone.current
        f.dateFormat = format
        return f
    }

    /// Today as YYYY-MM-DD (what Notion's date filter expects).
    static func iso(_ date: Date = Date()) -> String {
        formatter("yyyy-MM-dd").string(from: date)
    }

    static func startOfTomorrow() -> Date {
        let cal = Calendar.current
        return cal.date(byAdding: .day, value: 1, to: cal.startOfDay(for: Date())) ?? Date().addingTimeInterval(86_400)
    }

    static func parseDate(_ iso: String) -> Date? {
        formatter("yyyy-MM-dd").date(from: iso)
    }

    /// Parses the first 16 characters of a Notion date-time ("2026-09-26T14:00")
    /// as wall-clock time, ignoring the offset — same as the Android widget.
    static func parseLocalDateTime(_ s: String) -> Date? {
        formatter("yyyy-MM-dd'T'HH:mm").date(from: s)
    }
}

/// Port of TasksWidgetService.dueLabel: overdue → "Sep 3 · overdue" (red);
/// has a time → "2:00 PM"; otherwise "Today".
enum DueLabel {
    static func forWidget(_ due: String?) -> (text: String, overdue: Bool) {
        guard let due = due, due.count >= 10 else { return ("", false) }
        let datePart = String(due.prefix(10))
        if datePart < LocalDay.iso() {
            return ("\(monthDay(datePart)) · overdue", true)
        }
        if due.count > 10, let time = LocalDay.parseLocalDateTime(String(due.prefix(16))) {
            let f = DateFormatter()
            f.setLocalizedDateFormatFromTemplate("h:mm a")
            return (f.string(from: time), false)
        }
        return ("Today", false)
    }

    private static func monthDay(_ iso: String) -> String {
        guard let d = LocalDay.parseDate(iso) else { return iso }
        let f = DateFormatter()
        f.setLocalizedDateFormatFromTemplate("MMM d")
        return f.string(from: d)
    }
}

/// North Lafayette palette (android/app/src/main/res/values/colors.xml).
enum Palette {
    static let accent = Color(red: Double(0x14) / 255, green: Double(0x3B) / 255, blue: Double(0x7B) / 255)
    static let overdue = Color(red: Double(0xDC) / 255, green: Double(0x26) / 255, blue: Double(0x26) / 255)
}
