#!/usr/bin/env python3
"""Fetch fonts for compose.py into build/screenshot-bridge/fonts (not committed).
- Noto Sans CJK Regular/Bold .ttc: copied from the system (Linux: fonts-noto-cjk).
- Material Icons Round: from npm @material-design-icons/font, woff2 -> ttf (needs fonttools + brotli).
"""
import glob, os, shutil, subprocess, sys, tarfile, tempfile
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.environ.get("STORE_SHOTS_FONTS", os.path.join(HERE, "..", "..", "..", "..", "build", "screenshot-bridge", "fonts"))
os.makedirs(OUT, exist_ok=True)
for w in ("Regular", "Bold"):
    hits = glob.glob(f"/usr/share/fonts/**/NotoSansCJK-{w}.ttc", recursive=True) + glob.glob(os.path.expanduser(f"~/Library/Fonts/NotoSansCJK-{w}.ttc"))
    if not hits: sys.exit(f"NotoSansCJK-{w}.ttc not found; install Noto Sans CJK first")
    shutil.copy(hits[0], OUT)
from fontTools.ttLib import TTFont
with tempfile.TemporaryDirectory() as t:
    subprocess.run(["npm", "pack", "@material-design-icons/font@0.14.15"], cwd=t, check=True, capture_output=True)
    tarfile.open(glob.glob(os.path.join(t, "*.tgz"))[0]).extractall(t)
    f = TTFont(os.path.join(t, "package", "material-icons-round.woff2")); f.flavor = None
    f.save(os.path.join(OUT, "mi-round.ttf"))
print("fonts ready in", os.path.abspath(OUT))
