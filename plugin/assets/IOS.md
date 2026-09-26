# Sweep for iPhone — install via TestFlight

The iOS version of the Sweep widgets app is distributed through Apple's
TestFlight (iOS 17 or later).

**TestFlight link:** _not published yet — the first build is pending
(tracked in Linear NOR2-354). Update this line with the public TestFlight
link once the external group is live._

## Install

1. Install Apple's **TestFlight** app from the App Store if you don't have it.
2. Open the TestFlight link above on your iPhone and tap **Install**.
3. Open **Sweep**, paste your Notion connection token → **Load databases** →
   pick your Inbox and Tasks databases and the Done/Due properties → **Save**.
4. Long-press your home screen → **+** (top-left) → search **Sweep** → add
   the **Quick capture** and **Tasks due today** widgets.

## Connect your Notion

Same as Android — see "Connect your Notion" in [ANDROID.md](ANDROID.md):
Notion **Settings → Developer tools → Connections → New connection**, then
in its **Content access** tab add only your Inbox and Tasks databases, and
copy the secret token into the app on your phone. Only content you add there
is visible to the app.

## Notes

- Tap the circle on a task in the widget to check it off; it updates Notion
  right away. The list refreshes about every 30 minutes and right after
  midnight; the ↻ button refreshes now.
- Voice capture: tap the mic in the widget, then use the keyboard's mic key
  to dictate. You always get one look at the text before saving.
- Source and build instructions: `ios/` in
  https://github.com/jasonfletchall1/sweep-and-route.
