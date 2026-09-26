# Sweep for iOS — Notion inbox capture + tasks widgets

The iOS half of the Sweep phone app, a port of [`../android`](../android/).
Same two home-screen widgets, same Notion-only architecture:

- **Quick capture** — tap the widget, type (or use the keyboard's mic key to
  dictate), Save; the text lands as a new row in your Notion Inbox database.
- **Tasks due today** — a live list of open tasks due today or overdue,
  straight from Notion. Tap the circle to check a task off on the home
  screen (iOS 17 interactive widgets). `All ›` opens the full open-task list.

No accounts, no servers, no analytics. Your Notion token is stored only in
the app's private App Group storage on your phone, and the app talks only to
`api.notion.com`. Each user creates their own Notion connection scoped to just
their Inbox and Tasks databases — the same guided step as Android (see
[`ANDROID.md`](../plugin/assets/ANDROID.md), "Connect your Notion").

## How it's built

| Piece | Tech | Where |
| --- | --- | --- |
| App shell (settings, capture sheet, all-tasks list) | Expo SDK 57 / React Native, TypeScript, no navigation lib | `App.tsx`, `src/` |
| Notion client (app side) | `fetch`, port of `NotionApi.kt` | `src/notion.ts` |
| Config storage shared with the widgets | Local Expo Module writing the App Group's `UserDefaults` | `modules/sweep-shared/` |
| Home-screen widgets | SwiftUI + WidgetKit + AppIntents, generated as an app-extension target by [`@bacons/apple-targets`](https://github.com/EvanBacon/expo-apple-targets) | `targets/widgets/` |
| Build + TestFlight | EAS Build (cloud macOS) + EAS Submit — no Mac needed | `eas.json` |

Widgets on iOS have to be native SwiftUI (WidgetKit), so the widget code is
Swift even though the app shell is React Native. The Swift side is a direct
port of `TasksWidget.kt` / `TasksWidgetService.kt`: query open tasks due on
or before today's local date, cache the last good list keyed by the day it
was fetched for, refresh every ~30 minutes and right after midnight, and
tick only the Done checkbox when completing (so the Claude sweep's
"Completed on" stamping still works).

The app and the widgets share `group.us.northlafayette.sweep` and use the
same storage keys as the Android app's SharedPreferences (`token`,
`inboxDbId`, `inboxTitleProp`, `tasksDbId`, `doneProp`, `dueProp`).

Deep links the widgets use: `sweep://capture`, `sweep://capture?voice=1`,
`sweep://tasks`.

## Building and shipping to TestFlight (from Windows or anywhere)

Prerequisites: an [Expo account](https://expo.dev) (`eas login`), an Apple
Developer Program membership, and `npm i -g eas-cli`.

### One-time

1. **Apple Team ID** → `app.json` → `expo.ios.appleTeamId` (10 characters,
   from https://developer.apple.com/account → Membership details). The
   widget extension is signed as a second target and needs it.
2. `npm install`
3. `eas init` — links the project to your Expo account (creates
   `expo.extra.eas.projectId` in `app.json`; commit it).
4. First build: `eas build --platform ios --profile production`. EAS asks you
   to sign in with your Apple ID once, then registers both bundle IDs
   (`us.northlafayette.sweep` and `.widgets`), creates the distribution
   certificate and provisioning profiles, and stores them on EAS. Say yes to
   letting it manage credentials.
5. `eas submit --platform ios --latest` — uploads to App Store Connect. It can
   create the app record for you if it doesn't exist; otherwise create it at
   https://appstoreconnect.apple.com (name **Sweep**, bundle ID above) and
   pass `--asc-app-id`. Processing takes ~10–15 min, then the build shows up
   under TestFlight.
6. In App Store Connect → TestFlight: add internal testers (up to 100, no
   Apple review), or create an **external** group with a public link (needs
   a short Beta App Review, usually < 24 h). Fill "What to Test" with the
   Notion-connection steps from ANDROID.md.

Signing credentials live on EAS, not in this repo. Never commit `.p8`,
`.p12` or `.mobileprovision` files (already gitignored).

### Every release after that

```bash
cd ios
eas build --platform ios --profile production   # build number auto-increments
eas submit --platform ios --latest
```

Bump `expo.version` in `app.json` for user-visible version changes.

### Local checks that work without a Mac

```bash
npx tsc --noEmit          # type-check the React Native side
npx expo config --type introspect   # see the resolved config the plugins produce
```

`npx expo prebuild -p ios` is skipped on Windows; EAS runs it on its macOS
builder (`--clean`, then CocoaPods), which is where the Swift in
`targets/widgets/` first compiles. If a build fails, read the Xcode log in
the EAS build page — the widget target is called `SweepWidgets`.

## Known limitations vs. Android (v1)

- **No share-sheet target yet.** Sharing text from another app into the
  Inbox needs a Share Extension (another `targets/` entry); planned.
- **No dedicated speech recognizer.** Voice capture uses the iOS keyboard's
  built-in dictation key; a Siri / Shortcuts "Add to Sweep" intent is planned
  so you can capture hands-free.
- **Widget refresh is budgeted by iOS.** The timeline asks for a refresh every
  30 minutes and after midnight; iOS may coalesce these. The ↻ button and
  opening the app force one.
- iOS 17 or later (interactive widgets).
