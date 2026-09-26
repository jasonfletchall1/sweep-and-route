import SwiftUI
import WidgetKit

struct CaptureEntry: TimelineEntry {
    let date: Date
}

/// Static widget — it never changes, it just deep-links into the app.
struct CaptureProvider: TimelineProvider {
    func placeholder(in context: Context) -> CaptureEntry { CaptureEntry(date: Date()) }

    func getSnapshot(in context: Context, completion: @escaping (CaptureEntry) -> Void) {
        completion(CaptureEntry(date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<CaptureEntry>) -> Void) {
        completion(Timeline(entries: [CaptureEntry(date: Date())], policy: .never))
    }
}

struct CaptureWidgetView: View {
    @Environment(\.widgetFamily) private var family

    var body: some View {
        Group {
            if family == .systemSmall {
                // Small widgets get a single tap target.
                VStack(spacing: 8) {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 36))
                        .foregroundStyle(Palette.accent)
                    Text("Add to Inbox")
                        .font(.subheadline.weight(.semibold))
                    Text("Sweep")
                        .font(.caption2)
                        .foregroundStyle(Color.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .widgetURL(URL(string: "sweep://capture"))
            } else {
                // Medium: a field-looking tap target plus a mic, like the Android widget.
                HStack(spacing: 12) {
                    Link(destination: URL(string: "sweep://capture")!) {
                        HStack(spacing: 8) {
                            Image(systemName: "plus")
                                .foregroundStyle(Palette.accent)
                            Text("Add to Inbox…")
                                .foregroundStyle(Color.secondary)
                            Spacer(minLength: 0)
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 14, style: .continuous)
                                .fill(.quaternary)
                        )
                    }
                    Link(destination: URL(string: "sweep://capture?voice=1")!) {
                        Image(systemName: "mic.fill")
                            .font(.title3)
                            .foregroundStyle(.white)
                            .frame(width: 44, height: 44)
                            .background(Circle().fill(Palette.accent))
                    }
                    .accessibilityLabel("Voice capture")
                }
            }
        }
        .containerBackground(.background, for: .widget)
    }
}

struct CaptureWidget: Widget {
    static let kind = "SweepCapture"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: CaptureWidget.kind, provider: CaptureProvider()) { _ in
            CaptureWidgetView()
        }
        .configurationDisplayName("Quick capture")
        .description("One tap to add a thought to your Notion Inbox.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
