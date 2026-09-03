#!/usr/bin/env python3
"""Zip plugin/ into sweep-and-route.plugin at the repo root.

Zip entry paths must use forward slashes — Claude's plugin installer rejects
archives with backslash paths (which e.g. PowerShell's Compress-Archive
produces), so always package with this script or Info-ZIP's `zip -r`.
"""
import os
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "plugin")
DST = os.path.join(ROOT, "sweep-and-route.plugin")

apk = os.path.join(SRC, "assets", "sweep.apk")
if not os.path.exists(apk):
    sys.exit("plugin/assets/sweep.apk is missing — build the Android app and "
             "copy the release APK there first (see README).")

if os.path.exists(DST):
    os.remove(DST)

with zipfile.ZipFile(DST, "w", zipfile.ZIP_DEFLATED) as z:
    for root, dirs, files in os.walk(SRC):
        for f in sorted(files):
            full = os.path.join(root, f)
            arc = os.path.relpath(full, SRC).replace(os.sep, "/")
            z.write(full, arc)

with zipfile.ZipFile(DST) as z:
    names = z.namelist()
    assert not any("\\" in n for n in names), "backslash entry paths!"
    assert ".claude-plugin/plugin.json" in names, "manifest missing!"

print(f"wrote {DST} ({os.path.getsize(DST)} bytes, {len(names)} entries)")
