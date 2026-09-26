# Sweep & Route

**Capture anything with zero friction. Let Claude do the filing.**

Sweep & Route is a personal capture system built on [Notion](https://notion.com)
and [Claude](https://claude.com), with optional Android and iOS home-screen
widgets:

1. **Capture** — every thought, task, or commitment goes into one Notion
   **Inbox** database: from Notion itself, from the Sweep phone widget (one
   tap or voice), or from Android's share sheet.
2. **Sweep** — a scheduled Claude task runs hourly, classifies each capture,
   and routes it where it belongs:
   - tasks → a **Tasks** database, with due dates resolved from natural
     language ("Friday", "tomorrow")
   - ideas → an **Ideas** database
   - scheduled commitments → your **Google Calendar** *(optional)*
   - meeting agenda topics → the matching meeting's notes page *(optional)*
   - software work → **Linear** issues *(optional; the task checks itself off when the issue is Done or Canceled)*
   - action items from your meeting notes — **Granola**, **Gemini** notes via
     Google Drive, or similar — → Tasks *(optional)*
3. **See it** — the Sweep phone app (Android, and iOS via TestFlight) puts a
   live "due today" widget on your home screen with tap-to-complete, plus a
   one-tap capture widget.

## Quick start

You need a [Claude](https://claude.com) account with the **Notion connector**
connected.

1. Download `sweep-and-route.plugin` from the
   [latest release](https://github.com/jasonfletchall1/sweep-and-route/releases/latest).
2. Install it as a plugin in the Claude desktop app (Cowork: drop the file
   into a chat and accept it, or add it via the Customize page).
3. In a fresh session, tell Claude: **"set up sweep and route"** — it creates
   (or adopts) the Notion databases, schedules the sweep, and walks you
   through the optional phone app: on Android the `sweep.apk` bundled inside
   the plugin (also attached to the release for direct download); on iOS a
   TestFlight build (see [`ios/README.md`](ios/README.md)).

The phone apps talk only to `api.notion.com`, store your Notion token only in
app-private storage on your phone, and have no accounts, analytics, or
servers. Each user creates their own Notion connection scoped to just their
Inbox and Tasks databases.

## Repository layout

- [`plugin/`](plugin/) — the Claude plugin: the guided `setup` skill and the
  parameterized sweep-task prompt template. See its
  [README](plugin/README.md).
- [`android/`](android/) — the Sweep Android app (Kotlin, framework widgets,
  OkHttp; minSdk 26). See its [README](android/README.md).
- [`ios/`](ios/) — the Sweep iOS app (Expo/React Native shell + SwiftUI
  WidgetKit widgets, built and shipped to TestFlight with EAS; iOS 17+). See
  its [README](ios/README.md).
- [`scripts/package-plugin.py`](scripts/package-plugin.py) — zips `plugin/`
  into an installable `.plugin` file (zip entries must use forward slashes;
  don't use tools that write backslashes).

## Building

**Android app** (JDK 17 + Android SDK 34):

```bash
cd android && gradle assembleRelease
```

Release APKs from this repo's maintainers are signed for sideload-update
continuity; the keystore is deliberately **not** in the repo. Your own build
gets your own signature — phones with the official APK must uninstall before
installing a differently-signed build.

**iOS app** (no Mac needed — EAS builds in the cloud):

```bash
cd ios && npm install && eas build --platform ios --profile production
```

See [`ios/README.md`](ios/README.md) for the one-time Apple/EAS setup and the
TestFlight submit step.

**Plugin**: copy your built APK to `plugin/assets/sweep.apk`, then:

```bash
python scripts/package-plugin.py
```

## License

MIT — see [LICENSE](LICENSE).
