---
name: setup
description: Set up the Sweep and Route capture system for this user — create or adopt the Notion Inbox/Tasks/Ideas databases, schedule the recurring Claude sweep, and optionally install the Sweep Android widgets app. Use when the user says "set up sweep and route", "sweep setup", "onboard me to sweep", "install the sweep system", or asks how to get the inbox/sweep/widgets working on their account.
---

# Sweep and Route — setup

You are onboarding a user onto Sweep and Route: one Notion Inbox for
zero-friction capture, a scheduled Claude sweep that classifies and routes
captures, and (optionally) Android home-screen widgets. Run the steps in
order, confirming with the user at each decision point. Everything is created
in THEIR accounts — never reuse IDs, URLs, or tokens from these instructions'
author or any example.

This skill runs in Claude Cowork or Claude Code. Most users are on Cowork and
are not developers: keep every user-facing message in plain language — what
the system will do for them, never file paths, tool names, or cron syntax.
Use the capabilities the CURRENT session actually has (check, don't assume),
and where a capability is missing, degrade to clear instructions the user can
follow themselves.

## Step 0 — Check connectors

Required: **Notion**. If it is not connected, stop and tell the user to
connect it (claude.ai → Settings → Connectors) and re-run this skill.

Optional (ask which they want; skip the matching features cleanly if absent):
- **Google Calendar** → calendar routing for scheduled commitments
- **Meeting notes** → sync + action-item extraction. This is a CHOICE of
  source, not one tool — ask what they use for meeting notes and support any
  combination:
  - **Granola** (connector) — list/fetch meetings directly.
  - **Gemini "take notes for me"** (or any tool that saves notes docs to
    Google Drive) — needs the **Google Drive** connector. Ask which folder
    the notes land in (Gemini's default is "Meet Recordings"); verify you can
    see it and that it contains notes docs before wiring it in.
  - **Another connector they have** (e.g. Fireflies) — write a source block
    for it in the sweep prompt modeled on the Granola/Drive blocks: list
    meetings since last week, fetch the summary, same dedup rules.
  - **None** — Parts B/C are dropped entirely; the Inbox sweep still works.
  If they pick more than one source, the prompt's cross-source dedup rules
  matter — keep them intact.
- **Linear** → auto-filed issues for software-shaped tasks (ask which team;
  resolve their Linear user ID for assignments — never assign to "me", which
  can resolve to an integration account)

## Step 1 — Notion databases

First search their workspace for existing databases named like Inbox / Tasks /
Ideas. **Prefer adopting an existing database over creating a duplicate** —
show what you found and ask. Create whatever is missing:

- **📥 Inbox** — properties: `Item` (title), `Type` (select: Task, Idea,
  Agenda topic, Note), `Status` (select: New, Filed, Needs decision),
  `Routed to` (url), `Captured` (created time). Views: "Inbox" filtered to
  Status empty/New/Needs decision; "Recent" (rolled by the sweep).
- **✅ Tasks** — properties: `Task name` (title), `Done` (checkbox — the ONLY
  completion signal; do not add a Status property), `Assignee` (person),
  `Due` (date), `Completed on` (date), and `Linear` (url) only if Linear is
  connected. Views: "Open Tasks" (Done unchecked, sorted by Due), "Due Today"
  and "Completed" (rolled by the sweep).
- **💡 Ideas** — `Idea` (title), `Category` (select: App, Business, Content,
  Other), `Source` (text).
- **🗓️ Events Notes** (offer, optional) — `Event` (title), `Date` (date),
  `Type` (select: Conference, Meetup, Networking, Workshop, Other),
  `Location` (text), `Notes` (text), `Follow up` (checkbox).
- **📝 Meeting Notes** (only if a meeting-notes source was chosen) — `Title`
  (title), `Date` (date), `Link` (url), `Attendees` (multi-select),
  `Source` (select: one option per configured tool, e.g. Granola, Gemini).

Connector gotchas (learned the hard way — respect them):
- View filters accept only fixed ISO dates, not relative ones; the scheduled
  sweep rolls the date-windowed views forward each run.
- The Notion connector cannot delete pages or status-type properties; design
  around that (age rows out of views instead).
- `date:<prop>:is_datetime` must be the JSON integer `0`/`1`, not a string.

Also resolve the user's own Notion person ID (for task Assignee).

## Step 2 — Schedule the sweep

Build the sweep prompt from `references/sweep-task-prompt.md` in this skill's
directory: fill every `{{PLACEHOLDER}}` with this user's real values, keep
only the meeting-notes source blocks they chose (adding a custom block for a
source like Fireflies if that's what they use), and DELETE the sections
marked for connectors the user doesn't have.

Then schedule it with whatever scheduling surface THIS session offers — check
what is actually available rather than assuming:
- **Desktop app scheduled tasks** (Cowork and Claude Code both have these):
  create a task (id suggestion: `inbox-sweep-and-route`) running **hourly
  during waking hours**, e.g. cron `0 7-22 * * *` in local time. Hourly is
  the default because some surfaces (Cowork among them) reject anything more
  frequent than once per hour; only offer a faster cadence like every 30
  minutes if this session's scheduler demonstrably accepts it and the user
  wants it.
- **Cloud routines / scheduled agents** (claude.ai), if that's what the
  session has: same prompt, same cadence; the user's connectors must be
  authorized on claude.ai for the routine to use them.
- **Neither available**: save the filled-in prompt where the user can get it
  (their outputs folder, or send it as a file) and explain in plain language
  how to create a recurring scheduled task with it.

Tell the user to run the task once now and approve the permission prompts it
raises, so future automatic runs never pause waiting for approval. Describe
the schedule to them as "checks your inbox every half hour during the day" —
not as cron.

## Step 3 — Android widgets app (optional)

The signed APK ships in this plugin at `assets/sweep.apk` (also
`assets/ANDROID.md` with the full app README). If the user wants it:

1. Get the APK and ANDROID.md into the user's hands with whatever
   file-delivery this session has — send them as files in the conversation
   if a send-file tool exists, otherwise copy them to the user's outputs
   folder and say where they are. They need the APK reachable from their
   phone (chat attachment, Drive, email to self — their choice).
2. **Notion connection for the app** — the app needs a Notion connection
   token. There is NO Notion API for creating connections, so this is a
   click-through step in Notion's **developer portal** (reachable from the
   Notion desktop app or the web — Settings → **Developer tools**; do NOT
   send users hunting through the general Settings → Connections list, that
   is the wrong place). Walk them through it:
   - Developer tools → **Connections** tab → **New connection**. Name it
     `Sweep`, pick their workspace, allow read/insert/update content.
   - In the connection's **Content access** tab → **Add pages & databases**
     → grant the Inbox and Tasks databases, and nothing else. This one
     screen is the whole grant — there is nothing to do on the pages
     themselves.
   - The secret token is on the connection's Configuration tab. It is
     theirs alone: they copy it and paste it directly into the Sweep app on
     their phone. **Never ask for the token in chat, never type it
     anywhere, never store it.**
   If a browser-control tool is available and the user asks, you may drive
   the clicks with them watching — but the token step stays theirs.
3. Sideload: copy the APK to the phone, tap to install (allow "install
   unknown apps" once), open Sweep, paste token → Load databases → pick the
   Inbox and Tasks databases and the Done/Due properties → Save → add the two
   widgets from the launcher's widget picker.

## Step 4 — Recap

Finish with a short summary: databases created/adopted (with links), the
schedule of the sweep, which optional routings are on (calendar / Granola /
Linear) and which are off, and what remains for the user to do by hand
(Run-now approval; the phone-side steps). Remind them capture is the whole
point: anything, however rough, goes in the Inbox — the sweep sorts it out.
