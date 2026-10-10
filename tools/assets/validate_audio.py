# Copyright (C) 2026 ToontownSuperForeverMVP and NULLSTAR contributors
# SPDX-License-Identifier: GPL-3.0-only

"""Decodes every bundled OGG and checks format, peak headroom and loudness.

Run from the repository root:  python -I tools/assets/validate_audio.py [report.json]
Exits non-zero when a file is not mono 44.1 kHz Vorbis, clips, is silent or is missing
from sounds.json.
"""
import json
import os
import sys

import numpy as np
import soundfile as sf

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nullstar")
SOUNDS = os.path.join(ASSETS, "sounds")

with open(os.path.join(ASSETS, "sounds.json"), encoding="utf-8") as handle:
    definitions = json.load(handle)
referenced = {entry if isinstance(entry, str) else entry["name"]
              for event in definitions.values() for entry in event["sounds"]}

report, failures = [], []
for directory, _, files in os.walk(SOUNDS):
    for name in sorted(files):
        if not name.endswith(".ogg"):
            continue
        path = os.path.join(directory, name)
        rel = os.path.relpath(path, SOUNDS).replace(os.sep, "/")[:-4]
        info = sf.info(path)
        data, rate = sf.read(path, dtype="float32")
        peak = float(np.max(np.abs(data)))
        rms = float(np.sqrt(np.mean(data ** 2)))
        entry = {"sound": "nullstar:" + rel, "seconds": round(info.duration, 3), "rate": rate,
                 "channels": info.channels, "subtype": info.subtype,
                 "peak_dbfs": round(20 * np.log10(peak + 1e-12), 2), "rms_dbfs": round(20 * np.log10(rms + 1e-12), 2)}
        report.append(entry)
        if info.channels != 1 or rate != 44100 or info.subtype != "VORBIS":
            failures.append(rel + ": expected mono 44.1 kHz Vorbis")
        if peak >= .999 or not np.isfinite(data).all():
            failures.append(rel + ": clipping or invalid samples")
        if rms < 1e-3:
            failures.append(rel + ": effectively silent")
        if "nullstar:" + rel not in referenced:
            failures.append(rel + ": not referenced by sounds.json")
for missing in sorted(r for r in referenced if not os.path.exists(os.path.join(SOUNDS, r.split(":", 1)[1] + ".ogg"))):
    failures.append(missing + ": referenced but missing")

summary = {"files": len(report), "failures": failures, "sounds": report}
text = json.dumps(summary, indent=2)
if len(sys.argv) > 1:
    with open(sys.argv[1], "w", encoding="utf-8") as handle:
        handle.write(text + "\n")
print(text if failures else "%d sounds valid (mono 44.1 kHz Vorbis, peaks %.1f..%.1f dBFS)" % (
    len(report), min(e["peak_dbfs"] for e in report), max(e["peak_dbfs"] for e in report)))
sys.exit(1 if failures else 0)
