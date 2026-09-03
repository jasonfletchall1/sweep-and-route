# Sweep — Notion inbox capture + tasks widgets for Android

**This app is the phone-side half of the Sweep and Route system.** The other
half is the `sweep-and-route` Claude plugin, which you should install first:
its setup skill creates the Notion Inbox/Tasks databases for you, schedules
the Claude sweep that classifies and files everything you capture, and then
walks you through installing this app — including the Notion integration step
below. If you have the plugin, just tell Claude **"set up sweep and route"**
and let it drive; this README is the manual version of what it does, plus the
reference for the app itself.

Two home-screen widgets that talk straight to the Notion API, with none of the
Notion app's load time:

- **Quick capture** — one tap opens a small input box (mic button for voice);
  what you type lands as a new row in your Notion Inbox database. Also appears
  in Android's share sheet, so you can share any text into your Inbox.
- **Due today** — a live list of tasks due today or overdue. Tap the circle to
  check a task off right on the home screen. `All ›` opens your full open-task
  list.

No accounts, no servers, no analytics. Your Notion token is stored only in the
app's private storage on your phone, and the app talks only to `api.notion.com`.

## Install (sideload)

1. Copy the Sweep APK (`sweep.apk`) to your phone (Drive, email to yourself, USB — whatever).
2. Tap it. Android will ask you to allow installs from that app ("Install
   unknown apps") — allow it once.
3. Open **Sweep**.

## Connect your Notion

You need two databases in your Notion workspace: an **Inbox** (any database
with a title column — captures land as new rows) and a **Tasks** database with
a **checkbox** property for "done" and a **date** property for "due". The
Claude plugin's setup skill creates both (plus the sweep that files your
captures); without it, any two databases shaped like that will work.

1. Open Notion's developer portal — from the Notion desktop app or the web:
   **Settings** → **Developer tools** → **Connections** tab →
   **New connection**. Name it `Sweep`, pick your workspace, and allow
   read/insert/update content. (Don't look under the general Connections
   settings list — app connections are created only here.)
2. Still inside the `Sweep` connection, open its **Content access** tab →
   **Add pages & databases** → add your Inbox and Tasks databases. That one
   screen is the whole grant — nothing to configure on the pages themselves.
   **Only content you add there is visible to the app** — don't add anything
   else. Copy the secret token (`ntn_…`) from the Configuration tab.
3. In the Sweep app: paste the token → **Load databases** → pick your Inbox
   and Tasks databases and the Done/Due properties → **Save**.
4. Long-press your home screen → **Widgets** → add the two Sweep widgets.

## Notes

- The tasks widget auto-refreshes every ~30 minutes; the ↻ button refreshes
  immediately. Checking a task off updates Notion right away.
- "Due today" shows tasks whose due date is today **or earlier** (overdue shows
  in red). Tasks with no due date only appear in the `All ›` list.
- Voice capture: the first time you tap the mic, Sweep asks how you want to
  dictate — a speech-recognizer app on your device, or your own keyboard's
  voice input (Wispr Flow, Gboard's mic, etc.). Long-press the mic anytime to
  change your choice. You always get one look at the text before saving.

## Building from source

Requires JDK 17 and an Android SDK (platform 34). `local.properties` points at
the SDK. Then:

```
gradle assembleRelease
```

The signed APK lands in `app/build/outputs/apk/release/`. The signing keystore
in `keystore/` exists for install/update continuity when sideloading — if you
rebuild with a different keystore, phones with the old build must uninstall
first.
