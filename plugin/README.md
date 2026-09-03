# Sweep and Route

A universal capture system built on Notion + Claude, with optional Android
home-screen widgets. The idea: **capture with zero friction, let Claude do the
filing.**

## How it works

1. **Capture** — anything on your mind goes into one Notion **📥 Inbox**
   database: from Notion itself, from the Sweep Android widget (one tap or
   voice), or from Android's share sheet.
2. **Sweep** — a scheduled Claude task runs every 30 minutes, classifies each
   new capture, and routes it:
   - Tasks → **✅ Tasks** database (with due dates resolved from natural language)
   - Ideas → **💡 Ideas** database
   - Scheduled commitments → your **Google Calendar** (optional)
   - Meeting agenda topics → the matching meeting's notes page (optional)
   - Software work → **Linear** issues (optional)
   - Meeting action items from your meeting-notes tool → Tasks (optional).
     Sources are pluggable: **Granola** (connector), **Gemini "take notes for
     me"** or anything else that saves notes docs to a **Google Drive**
     folder, another connector you have (e.g. Fireflies) — or any mix, with
     dedup so one meeting never becomes two pages.
3. **See it** — the Sweep Android app puts a live "due today" widget on your
   home screen with in-widget check-off, plus the one-tap capture widget.

## Install

Grab the latest `sweep-and-route.plugin` from the [releases page](https://github.com/jasonfletchall1/sweep-and-route/releases/latest). The same file works in both Claude surfaces:

- **Claude Cowork** (most users): drop `sweep-and-route.plugin` into a Cowork
  chat and accept it from the preview, or add it via the desktop app's
  plugin settings.
- **Claude Code**: install the same file through the desktop app's plugin
  upload; it appears as the `sweep-and-route` plugin.

The scheduled sweep runs on whichever surface you set it up from — the setup
skill uses your session's own scheduling (Cowork and Claude Code desktop
scheduled tasks, or claude.ai routines) and never requires Claude Code
specifically.

## Get started

With this plugin installed and your **Notion connector** connected, tell
Claude:

> set up sweep and route

The `sweep-and-route:setup` skill will create the Notion databases (or adopt
ones you already have), schedule the sweep, and — if you want the widgets —
hand you the Android APK from this plugin and walk you through connecting it.

Optional connectors, each unlocking a feature: **Google Calendar** (calendar
routing), **Linear** (auto-filed issues), and a meeting-notes source —
**Granola**, **Google Drive** (for Gemini notes and similar), or another
notes connector. Connect them before running setup, or re-run setup later to
add them.

## What's in this plugin

- `skills/setup` — the guided onboarding skill
- `skills/setup/references/sweep-task-prompt.md` — the template for the
  scheduled sweep, parameterized per user
- `assets/sweep.apk` — the signed Android app (sideload; see the setup
  skill or `assets/ANDROID.md`)
