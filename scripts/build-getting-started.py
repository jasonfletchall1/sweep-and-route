"""Build the Sweep & Route getting-started PDF (plain-language README)."""
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import (
    KeepTogether, ListFlowable, ListItem, PageBreak, Paragraph,
    SimpleDocTemplate, Spacer, Table, TableStyle,
)

import os, sys
# Default: the repo root next to this scripts/ folder; pass a path to override.
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "Sweep-and-Route-Getting-Started.pdf")
RELEASES = "https://github.com/jasonfletchall1/sweep-and-route/releases/latest"

INK = colors.HexColor("#1f2933")
MUTED = colors.HexColor("#52606d")
ACCENT = colors.HexColor("#2f6f9f")
CALLOUT_BG = colors.HexColor("#eef4f9")
CALLOUT_EDGE = colors.HexColor("#b9d0e3")
WARN_BG = colors.HexColor("#fff6e5")
WARN_EDGE = colors.HexColor("#f0c674")
RULE = colors.HexColor("#d9dee3")

ss = getSampleStyleSheet()
base = ParagraphStyle("base", parent=ss["Normal"], fontName="Helvetica",
                      fontSize=10.5, leading=15, textColor=INK, spaceAfter=7)
title = ParagraphStyle("title", parent=base, fontName="Helvetica-Bold",
                       fontSize=26, leading=31, spaceAfter=4)
subtitle = ParagraphStyle("subtitle", parent=base, fontSize=13, leading=18,
                          textColor=MUTED, spaceAfter=18)
h1 = ParagraphStyle("h1", parent=base, fontName="Helvetica-Bold", fontSize=17,
                    leading=21, textColor=ACCENT, spaceBefore=16, spaceAfter=8)
h2 = ParagraphStyle("h2", parent=base, fontName="Helvetica-Bold", fontSize=12.5,
                    leading=16, spaceBefore=10, spaceAfter=5)
small = ParagraphStyle("small", parent=base, fontSize=9, leading=12.5,
                       textColor=MUTED)
callout_body = ParagraphStyle("callout", parent=base, spaceAfter=3)
callout_head = ParagraphStyle("callout_head", parent=base,
                              fontName="Helvetica-Bold", spaceAfter=3)
cell = ParagraphStyle("cell", parent=base, fontSize=9.5, leading=13, spaceAfter=0)
cell_b = ParagraphStyle("cell_b", parent=cell, fontName="Helvetica-Bold")


def P(text, style=base):
    return Paragraph(text, style)


def bullets(items, numbered=False):
    flow = [ListItem(P(t, callout_body), leftIndent=14) for t in items]
    return ListFlowable(
        flow, bulletType="1" if numbered else "bullet",
        start="1" if numbered else None, leftIndent=16,
        bulletFontName="Helvetica-Bold" if numbered else "Helvetica",
        bulletFontSize=10, bulletColor=ACCENT if numbered else INK,
        spaceAfter=6)


def box(heading, body_flowables, warn=False):
    """Shaded callout. body_flowables is a list of flowables."""
    inner = [P(heading, callout_head)] + body_flowables
    t = Table([[inner]], colWidths=[6.6 * inch])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), WARN_BG if warn else CALLOUT_BG),
        ("BOX", (0, 0), (-1, -1), 0.8, WARN_EDGE if warn else CALLOUT_EDGE),
        ("LEFTPADDING", (0, 0), (-1, -1), 12),
        ("RIGHTPADDING", (0, 0), (-1, -1), 12),
        ("TOPPADDING", (0, 0), (-1, -1), 9),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    return KeepTogether([t, Spacer(1, 8)])


def rule():
    t = Table([[""]], colWidths=[6.6 * inch], rowHeights=[1])
    t.setStyle(TableStyle([("LINEABOVE", (0, 0), (-1, 0), 0.6, RULE)]))
    return t


def on_page(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 8.5)
    canvas.setFillColor(MUTED)
    canvas.drawString(0.9 * inch, 0.55 * inch, "Sweep & Route  -  Getting Started")
    canvas.drawRightString(letter[0] - 0.9 * inch, 0.55 * inch, f"Page {doc.page}")
    canvas.restoreState()


story = []

# ---------------------------------------------------------------- cover / intro
story += [
    P("Sweep &amp; Route", title),
    P("Getting started - a plain-language guide for new users", subtitle),
    P("<b>The idea in one sentence:</b> you throw every thought, to-do, or "
      "commitment into one place, and Claude sorts it all into the right "
      "lists for you, automatically, about once an hour."),
    P("You never have to decide <i>where</i> something goes at the moment you "
      "think of it. That decision is the friction that makes most people stop "
      "writing things down. Sweep &amp; Route removes it."),
    Spacer(1, 6),
    P("How it works", h1),
    bullets([
        "<b>Capture.</b> Everything goes into a single list in Notion called "
        "the <b>Inbox</b>. You can add to it from Notion on any device, or - "
        "if you have an Android phone - from a home-screen widget with one "
        "tap or your voice.",
        "<b>Sweep.</b> About once an hour during the day, Claude reads what "
        "is new in the Inbox, works out what each item is, and files it. "
        "A to-do becomes a task with a due date (\"Friday\" and \"tomorrow\" "
        "are understood). An idea goes on your ideas list. An appointment "
        "can go on your calendar. A topic for a meeting can be added to that "
        "meeting's notes.",
        "<b>See it.</b> The optional phone app shows what is due today right "
        "on your home screen. Tap the circle next to a task to mark it done.",
    ]),
    P("Where things end up", h2),
]

routing = [
    [P("You capture...", cell_b), P("Claude files it to...", cell_b), P("", cell_b)],
    [P("A to-do", cell), P("Your <b>Tasks</b> list, with a due date if you gave one", cell), P("Always on", cell)],
    [P("An idea", cell), P("Your <b>Ideas</b> list", cell), P("Always on", cell)],
    [P("An appointment or event at a specific time", cell), P("Your <b>Google Calendar</b> (personal calendar only, never sends invites)", cell), P("Optional", cell)],
    [P("A topic to raise in an upcoming meeting", cell), P("That meeting's notes page", cell), P("Optional", cell)],
    [P("A piece of software work", cell), P("A <b>Linear</b> issue", cell), P("Optional", cell)],
    [P("Nothing - but your meeting-notes tool recorded action items", cell), P("Those action items become <b>Tasks</b> too (works with Granola, or Gemini notes saved to Google Drive)", cell), P("Optional", cell)],
    [P("Notes about an event you attended", cell), P("An <b>Events Notes</b> list", cell), P("Optional", cell)],
]
rt = Table(routing, colWidths=[2.1 * inch, 3.5 * inch, 1.0 * inch], repeatRows=1)
rt.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (-1, 0), CALLOUT_BG),
    ("LINEBELOW", (0, 0), (-1, 0), 0.8, CALLOUT_EDGE),
    ("LINEBELOW", (0, 1), (-1, -1), 0.4, RULE),
    ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ("LEFTPADDING", (0, 0), (-1, -1), 6),
    ("RIGHTPADDING", (0, 0), (-1, -1), 6),
    ("TOPPADDING", (0, 0), (-1, -1), 5),
    ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
]))
story += [rt, Spacer(1, 6),
          P("\"Optional\" features switch on only if you have connected that "
            "tool to Claude. If Claude cannot tell where something belongs, "
            "it marks the item <i>Needs decision</i> and leaves it in the "
            "Inbox for you.", small)]

# ---------------------------------------------------------------- before you start
story += [
    PageBreak(),
    P("Before you start", h1),
    P("You need three things. The rest is optional."),
    bullets([
        "<b>A Claude account</b>, using the Claude desktop app. Most people "
        "use <b>Claude Cowork</b>; <b>Claude Code</b> works too.",
        "<b>A Notion account.</b> Notion is where your Inbox, Tasks, and Ideas "
        "live. A free personal workspace is fine.",
        "<b>Notion connected to Claude.</b> Claude has to be allowed into your "
        "Notion before it can create lists there. See the box below.",
    ], numbered=True),
    box("You do this: connect Notion to Claude", [
        bullets([
            "Go to <b>claude.ai</b>, open <b>Settings</b>, then <b>Connectors</b>.",
            "Find <b>Notion</b> and connect it. Notion will ask you to sign in "
            "and approve access - approve it.",
        ], numbered=True),
        P("If you skip this, setup stops at the first step and asks you to "
          "do it, so it is easier to do it now.", small),
    ]),
    P("Optional extras", h2),
    P("Each of these unlocks one feature. Connect any you want the same way "
      "(claude.ai &rarr; Settings &rarr; Connectors) <b>before</b> running "
      "setup. You can always add one later and run setup again."),
    bullets([
        "<b>Google Calendar</b> - appointments you capture get added to your calendar.",
        "<b>Linear</b> - software work you capture becomes a Linear issue.",
        "<b>A meeting-notes tool</b> - action items from your meetings become "
        "tasks. This can be <b>Granola</b>, or <b>Google Drive</b> if you use "
        "Gemini's \"take notes for me\" (it saves notes to a Drive folder, "
        "usually called \"Meet Recordings\"). Other note-taking tools you "
        "have connected can often be wired in too.",
        "<b>An Android phone</b> - for the home-screen widgets. (The rest of "
        "the system works on any device through the Notion app; only the "
        "widgets are Android-only.)",
    ]),
]

# ---------------------------------------------------------------- setup
story += [
    P("Setting it up", h1),
    P("There are three steps, and Claude does most of the work in the third one."),
    P("Step 1 - Download the plugin", h2),
    P("A \"plugin\" is just a small file that teaches Claude how to set up "
      "and run Sweep &amp; Route. Download <b>sweep-and-route.plugin</b> from:"),
    P(f'<link href="{RELEASES}" color="#2f6f9f">{RELEASES}</link>'),
    P("(Grab it on the computer where you use the Claude desktop app.)", small),

    P("Step 2 - Install the plugin", h2),
    bullets([
        "<b>Claude Cowork:</b> drag the file into a chat and accept it when "
        "the preview appears. Or add it from the desktop app's Customize / "
        "plugin settings page.",
        "<b>Claude Code:</b> upload the same file through the desktop app's "
        "plugin settings. It shows up as <b>sweep-and-route</b>.",
    ]),

    P("Step 3 - Tell Claude to set it up", h2),
    P("Start a <b>fresh chat</b> and type exactly:"),
    box("set up sweep and route", [
        P("That's it. Claude takes it from here and will:", callout_body),
        bullets([
            "check which tools you have connected (Notion, plus any optional ones)",
            "look for Inbox / Tasks / Ideas lists you already have in Notion "
            "and offer to reuse them, or create new ones",
            "create the automatic hourly sweep",
            "offer to walk you through the phone app if you want it",
        ]),
    ]),
    P("Claude will ask you a few questions along the way. Expect these:"),
    bullets([
        "<b>Which optional features do you want?</b> (calendar, Linear, meeting notes)",
        "<b>Reuse an existing list, or make a new one?</b> If you already have "
        "something called Inbox or Tasks in Notion, Claude shows it and asks.",
        "<b>If you chose Linear:</b> which team, and whether you have an "
        "\"AI\" user in Linear that work should be handed to when you say "
        "\"have Claude do this\".",
        "<b>If you chose Gemini / Google Drive notes:</b> which folder the "
        "notes land in.",
        "<b>Do you want the Android app?</b> (See the next section.)",
    ]),
    box("You do this: run the sweep once by hand", [
        P("When Claude says the sweep is scheduled, <b>run it once yourself "
          "right away</b> and <b>approve every permission prompt</b> it "
          "raises (it will ask to read and write your Notion, calendar, and "
          "so on).", callout_body),
        P("This matters: the sweep runs on its own from then on, and an "
          "unapproved permission would make it stop and wait silently. "
          "Approving once means it never gets stuck.", callout_body),
    ], warn=True),
    P("Claude finishes with a short recap: which lists it made (with links), "
      "when the sweep runs, which optional features are on, and anything "
      "left for you to do by hand."),
]

# ---------------------------------------------------------------- phone app
story += [
    PageBreak(),
    P("The phone app (optional, Android only)", h1),
    P("The <b>Sweep</b> app adds two widgets to your home screen: a one-tap "
      "capture box (with a microphone for voice) and a live \"due today\" "
      "list you can check off in place. It talks directly to Notion, so "
      "there is no waiting for the Notion app to load."),
    P("It is not in the Play Store - you install it from a file. That takes "
      "four short parts. Claude walks you through them during setup, but "
      "here they are in full so nothing is lost."),

    P("Part A - Put the app on your phone", h2),
    bullets([
        "Get the app file onto your phone. Claude hands you <b>sweep.apk</b> "
        "during setup; you can also download <b>sweep-v1.1.2.apk</b> (or "
        "newer) from the releases page above. Send it to yourself however "
        "you like - email, Google Drive, a chat message, USB cable.",
        "On the phone, tap the file. Android will ask whether to allow "
        "installs from that app (\"Install unknown apps\") - allow it. You "
        "only need to do this once.",
        "Open <b>Sweep</b>. It will ask for a Notion \"token\" - that is what "
        "Part B produces.",
    ], numbered=True),

    P("Part B - Give the app its own key to your Notion", h2),
    P("The app needs a private key (Notion calls it a <i>connection</i>, and "
      "the key itself a <i>token</i>) so it can add to your Inbox and tick "
      "off tasks. You create this in Notion yourself - there is no way for "
      "Claude to do it for you."),
    box("You do this: create the connection in Notion", [
        bullets([
            "Open Notion on your computer (the desktop app or the website). "
            "Go to <b>Settings</b>, then <b>Developer tools</b>.",
            "Open the <b>Connections</b> tab and click <b>New connection</b>. "
            "Name it <b>Sweep</b>, pick your workspace, and allow it to "
            "<b>read, insert, and update content</b>.",
            "Still inside the Sweep connection, open its <b>Content access</b> "
            "tab and click <b>Add pages &amp; databases</b>. Add your "
            "<b>Inbox</b> and <b>Tasks</b> lists - <b>and nothing else</b>. "
            "The app can only see what you add here. There is nothing to do "
            "on the pages themselves.",
            "Open the connection's <b>Configuration</b> tab and copy the "
            "secret token (it starts with <b>ntn_</b>).",
        ], numbered=True),
        P("<b>Wrong turn to avoid:</b> the general \"Connections\" list in "
          "Notion's settings is <i>not</i> the place. App connections are only "
          "created under <b>Developer tools</b>.", small),
    ]),
    box("Keep the token to yourself", [
        P("The token is a password to those two lists. Paste it straight "
          "into the Sweep app on your phone and nowhere else. <b>Do not "
          "paste it into a chat with Claude</b> - Claude will never ask for "
          "it, and setup is designed so it never sees it.", callout_body),
    ], warn=True),

    P("Part C - Connect the app", h2),
    bullets([
        "In Sweep, paste the token and tap <b>Load databases</b>.",
        "Pick your <b>Inbox</b> and <b>Tasks</b> lists from the dropdowns, "
        "then pick which columns mean <b>Done</b> and <b>Due</b> (the ones "
        "setup created are called exactly that).",
        "Tap <b>Save</b>.",
    ], numbered=True),

    P("Part D - Add the widgets", h2),
    bullets([
        "Long-press an empty spot on your home screen and choose <b>Widgets</b>.",
        "Find <b>Sweep</b> and add both widgets: <b>Quick capture</b> and "
        "<b>Due today</b>.",
    ], numbered=True),

    P("Good to know about the app", h2),
    bullets([
        "The due-today list refreshes itself about every 30 minutes and again "
        "just after midnight. The &#8635; button refreshes it right now.",
        "\"Due today\" means due today <i>or earlier</i>; overdue items show in "
        "red. Tasks with no due date only appear under <b>All &rsaquo;</b>.",
        "Checking a task off updates Notion immediately.",
        "The first time you tap the microphone, Sweep asks how you want to "
        "dictate - a speech app on your phone, or your keyboard's own voice "
        "input. Long-press the mic later to change your mind. You always get "
        "to look at the text before it is saved.",
        "You can also share any text from any app into your Inbox - Sweep "
        "appears in Android's share menu.",
        "Privacy: no accounts, no servers, no tracking. The app talks only to "
        "Notion, and the token lives only on your phone.",
    ]),
]

# ---------------------------------------------------------------- day to day
story += [
    PageBreak(),
    P("Using it day to day", h1),
    bullets([
        "<b>Capture everything, however rough.</b> \"call dentist thurs\", "
        "\"idea: newsletter about X\", \"ask Sam about budget in Monday's "
        "meeting\". The sweep sorts it out. You do not need to tidy it first.",
        "<b>Check your Tasks list</b> (or the phone widget) to see what is "
        "due. <b>Ticking the Done box is the only thing that completes a "
        "task.</b> The sweep stamps a completion date the next time it runs, "
        "and the task moves from your open list to the completed list the "
        "following day, so it stays visible until the day turns over.",
        "<b>Glance at the Inbox now and then</b> for anything marked "
        "<i>Needs decision</i> - those are items Claude was not sure about. "
        "Filed items are marked <i>Filed</i> with a link to where they went.",
        "<b>Meeting notes</b> (if you set that up) get copied into a "
        "<b>Meeting Notes</b> list in Notion, with their action items turned "
        "into tasks. One meeting never becomes two pages, even if you use "
        "more than one note-taking tool.",
    ]),

    P("If something isn't working", h1),
    bullets([
        "<b>The sweep isn't filing anything.</b> Most often it is waiting on "
        "a permission it was never granted. Run the scheduled task once by "
        "hand and approve the prompts (see the orange box on page 3).",
        "<b>Setup said Notion isn't connected.</b> Connect it at claude.ai "
        "&rarr; Settings &rarr; Connectors, then start a new chat and type "
        "\"set up sweep and route\" again.",
        "<b>The widget is empty.</b> Only tasks with a due date of today or "
        "earlier show there. Tap &#8635; to refresh. Everything else is "
        "under <b>All &rsaquo;</b>.",
        "<b>The app can't see my lists.</b> Go back to the Sweep connection "
        "in Notion (Settings &rarr; Developer tools) and check that both "
        "Inbox and Tasks were added under <b>Content access</b>.",
        "<b>I want to add calendar / Linear / meeting notes later.</b> "
        "Connect the tool to Claude, then type \"set up sweep and route\" "
        "again. Claude will keep what you already have and add the new part.",
        "<b>Updating the app.</b> Install the newer official file over the "
        "top of the old one - no need to uninstall.",
    ]),

    P("Setup checklist", h1),
    P("Everything you personally need to do, in order. Tick them off."),
]

checklist = [
    "Connect Notion to Claude (claude.ai &rarr; Settings &rarr; Connectors)",
    "Optional: connect Google Calendar, Linear, Granola, or Google Drive the same way",
    "Download sweep-and-route.plugin from the releases page",
    "Install it in the Claude desktop app (Cowork: drop into a chat and accept)",
    "In a fresh chat, type: set up sweep and route",
    "Answer Claude's questions (features, reuse vs. create, Linear team, notes folder)",
    "Run the sweep once by hand and approve every permission prompt",
    "Phone (optional): put sweep.apk on the phone, tap to install, allow unknown apps once",
    "Phone: in Notion, Settings &rarr; Developer tools &rarr; Connections &rarr; New connection named Sweep",
    "Phone: Content access &rarr; Add pages &amp; databases &rarr; Inbox and Tasks only",
    "Phone: copy the ntn_ token from Configuration and paste it into the Sweep app only",
    "Phone: Load databases &rarr; pick Inbox, Tasks, Done, Due &rarr; Save",
    "Phone: long-press home screen &rarr; Widgets &rarr; add both Sweep widgets",
]
rows = [[P("&#9744;", cell), P(c, cell)] for c in checklist]
ct = Table(rows, colWidths=[0.35 * inch, 6.25 * inch])
ct.setStyle(TableStyle([
    ("LINEBELOW", (0, 0), (-1, -1), 0.4, RULE),
    ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ("LEFTPADDING", (0, 0), (-1, -1), 4),
    ("TOPPADDING", (0, 0), (-1, -1), 5),
    ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
]))
story += [ct, Spacer(1, 14), rule(), Spacer(1, 6),
          P("Sweep &amp; Route is free and open source (MIT). Code, releases, "
            f'and updates: <link href="https://github.com/jasonfletchall1/sweep-and-route" '
            'color="#2f6f9f">github.com/jasonfletchall1/sweep-and-route</link>', small)]

doc = SimpleDocTemplate(
    OUT, pagesize=letter, leftMargin=0.9 * inch, rightMargin=0.9 * inch,
    topMargin=0.85 * inch, bottomMargin=0.9 * inch,
    title="Sweep & Route - Getting Started", author="Sweep & Route",
    subject="Plain-language setup guide for new users")
doc.build(story, onFirstPage=on_page, onLaterPages=on_page)
print("wrote", OUT)
