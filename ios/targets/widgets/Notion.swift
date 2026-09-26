import Foundation

/// A task row as the widget needs it. Named NotionTask so it never collides
/// with Swift Concurrency's `Task`.
struct NotionTask: Codable, Identifiable, Hashable {
    let id: String
    let name: String
    let due: String?
    let url: String?
}

struct NotionError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

/// Minimal Notion REST client for the widget extension — the same two calls
/// the Android TasksWidget makes (query open tasks, tick the Done checkbox).
enum Notion {
    private static let base = "https://api.notion.com/v1/"
    private static let version = "2022-06-28"

    private static func call(_ method: String, _ path: String, token: String, body: [String: Any]?) async throws -> [String: Any] {
        guard let url = URL(string: base + path) else { throw NotionError(message: "Bad URL") }
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.timeoutInterval = 25
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue(version, forHTTPHeaderField: "Notion-Version")
        req.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        if method != "GET" {
            req.httpBody = try JSONSerialization.data(withJSONObject: body ?? [:])
        }
        let (data, resp) = try await URLSession.shared.data(for: req)
        let status = (resp as? HTTPURLResponse)?.statusCode ?? 0
        let json = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
        guard (200..<300).contains(status) else {
            let apiMessage = json["message"] as? String ?? ""
            throw NotionError(message: apiMessage.isEmpty ? "HTTP \(status)" : apiMessage)
        }
        return json
    }

    static func plainText(_ value: Any?) -> String {
        guard let parts = value as? [[String: Any]] else { return "" }
        return parts.compactMap { $0["plain_text"] as? String }.joined()
    }

    /// Open tasks (done checkbox unchecked), sorted by due date ascending.
    /// With `dueOnOrBefore` (ISO date) only tasks due on/before that day are
    /// returned — which also excludes tasks with no due date at all.
    static func openTasks(
        token: String,
        databaseId: String,
        doneProp: String,
        dueProp: String,
        dueOnOrBefore: String?
    ) async throws -> [NotionTask] {
        var and: [[String: Any]] = [["property": doneProp, "checkbox": ["equals": false]]]
        if let day = dueOnOrBefore {
            and.append(["property": dueProp, "date": ["on_or_before": day]])
        }
        let body: [String: Any] = [
            "filter": ["and": and],
            "sorts": [["property": dueProp, "direction": "ascending"]],
            "page_size": 100,
        ]
        let res = try await call("POST", "databases/\(databaseId)/query", token: token, body: body)
        let results = res["results"] as? [[String: Any]] ?? []
        return results.compactMap { page -> NotionTask? in
            guard let id = page["id"] as? String,
                  let props = page["properties"] as? [String: Any]
            else { return nil }
            var name = ""
            for (_, raw) in props {
                if let p = raw as? [String: Any], (p["type"] as? String) == "title" {
                    name = plainText(p["title"])
                    break
                }
            }
            if name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { name = "(untitled)" }
            let dueRaw = ((props[dueProp] as? [String: Any])?["date"] as? [String: Any])?["start"] as? String
            let urlRaw = page["url"] as? String
            return NotionTask(
                id: id,
                name: name,
                due: (dueRaw ?? "").isEmpty ? nil : dueRaw,
                url: (urlRaw ?? "").isEmpty ? nil : urlRaw
            )
        }
    }

    static func completeTask(token: String, pageId: String, doneProp: String) async throws {
        _ = try await call("PATCH", "pages/\(pageId)", token: token, body: [
            "properties": [doneProp: ["checkbox": true]],
        ])
    }
}
