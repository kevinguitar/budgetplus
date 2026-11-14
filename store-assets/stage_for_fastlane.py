#!/usr/bin/env python3
"""Lay out store-assets/screenshots/ for fastlane and check it.

usage: stage_for_fastlane.py OUT_DIR [--target both|ios|android]
       [--skip-screenshots] [--skip-creative-assets]

Writes:
  OUT_DIR/ios/<asc-locale>/<NN>-<device>-<screen>.png        (deliver screenshots_path)
  OUT_DIR/android/<play-locale>/images/<type>/<NN>.png       (supply metadata_path)
  OUT_DIR/android/<play-locale>/images/featureGraphic.png    (from screenshots/android/creative/)

Checks every file is an RGB PNG (no alpha), sizes are uniform per device class,
per-type counts stay within store limits (App Store 10, Google Play 8), and the
files follow ORDER. Staged names keep the NN- prefix, which sets the display order.

Also checks the creative art: the Google Play feature graphic (1024x500, staged above; supply
uploads it unless skip_upload_images) and the App Store header and search results art in
screenshots/ios/creative/ (not staged: fastlane doesn't upload it; upload_creative_assets.py does).
"""
import argparse, os, shutil, struct, sys
from upload_creative_assets import check_creative

HERE = os.path.dirname(os.path.abspath(__file__))
FINAL = os.path.join(HERE, "screenshots")
LOCALES = {  # repo locale -> (App Store Connect, Google Play)
    "en-US": ("en-US", "en-US"),
    "zh-Hant": ("zh-Hant", "zh-TW"),
    "zh-Hans": ("zh-Hans", "zh-CN"),
    "ja": ("ja", "ja-JP"),
    "ko": ("ko", "ko-KR"),
}
IOS = ["iphone-6.3", "iphone-duo-outer", "ipad-13"]  # deliver detects the display type from the pixel size
ANDROID = {"phone": "phoneScreenshots", "tablet": "tenInchScreenshots"}
LIMIT = {"ios": 10, "android": 8}
# Store display order. Both App Store Connect (deliver re-sorts each set by file name)
# and Google Play (supply uploads in sorted file-name order) follow the NN- prefix.
ORDER = ["01-login", "02-books", "03-shared", "04-currency", "05-overview", "06-tones", "07-colors", "08-share"]

def png_info(path):
    with open(path, "rb") as f:
        head = f.read(26)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"{path}: not a PNG")
    w, h = struct.unpack(">II", head[16:24])
    return w, h, head[25]  # color type: 2 = RGB, 6 = RGBA

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("out")
    parser.add_argument("--target", choices=("both", "ios", "android"), default="both")
    parser.add_argument("--skip-screenshots", action="store_true")
    parser.add_argument("--skip-creative-assets", action="store_true")
    args = parser.parse_args()
    out = args.out
    # Never remove an existing directory (including source assets) on a rerun.
    os.makedirs(out, exist_ok=False)
    errors, summary = [], []
    for platform, classes in (("ios", IOS), ("android", list(ANDROID))):
        if args.target != "both" and platform != args.target: continue
        if args.skip_screenshots: continue
        for cls in classes:
            sizes = set()
            for loc, (asc, play) in LOCALES.items():
                src = os.path.join(FINAL, platform, cls, loc)
                files = sorted(f for f in os.listdir(src) if f.endswith(".png")) if os.path.isdir(src) else []
                if not files: errors.append(f"missing {platform}/{cls}/{loc}"); continue
                if len(files) > LIMIT[platform]: errors.append(f"{platform}/{cls}/{loc}: {len(files)} > {LIMIT[platform]}")
                if [f[:-4] for f in files] != ORDER[:len(files)]:
                    errors.append(f"{platform}/{cls}/{loc}: files {[f[:-4] for f in files]} don't match the order {ORDER}")
                for f in files:
                    w, h, ct = png_info(os.path.join(src, f)); sizes.add((w, h))
                    if ct != 2: errors.append(f"{platform}/{cls}/{loc}/{f}: color type {ct}, expected RGB without alpha")
                    if platform == "ios":
                        dst = os.path.join(out, "ios", asc, f"{f[:2]}-{cls}-{f[3:]}")
                    else:
                        dst = os.path.join(out, "android", play, "images", ANDROID[cls], f)
                    os.makedirs(os.path.dirname(dst), exist_ok=True); shutil.copy(os.path.join(src, f), dst)
                summary.append(f"{platform:8} {cls:18} {loc:8} -> {asc if platform == 'ios' else play:8} {len(files)} files")
            if len(sizes) != 1: errors.append(f"{platform}/{cls}: mixed sizes {sorted(sizes)}")
            else: summary.append(f"{platform:8} {cls:18} size {sizes.pop()}")
    if args.target != "ios" and not args.skip_creative_assets:
        for loc, (asc, play) in LOCALES.items():                 # Google Play feature graphic
            src = os.path.join(FINAL, "android", "creative", loc, "featureGraphic.png")
            if not os.path.isfile(src): errors.append(f"missing android/creative/{loc}/featureGraphic.png"); continue
            w, h, ct = png_info(src)
            if (w, h) != (1024, 500): errors.append(f"android/creative/{loc}/featureGraphic.png: {w}x{h}, expected 1024x500")
            if ct != 2: errors.append(f"android/creative/{loc}/featureGraphic.png: color type {ct}, expected RGB without alpha")
            dst = os.path.join(out, "android", play, "images", "featureGraphic.png")
            os.makedirs(os.path.dirname(dst), exist_ok=True); shutil.copy(src, dst)
            summary.append(f"android  creative           {loc:8} -> {play:8} featureGraphic")
    if args.target != "android" and not args.skip_creative_assets:
        creative_errors, creative_summary = check_creative()
        errors += creative_errors; summary += creative_summary
    print("\n".join(summary))
    if errors:
        print("\nERRORS:\n" + "\n".join(errors)); sys.exit(1)
    print("\nOK")

if __name__ == "__main__":
    main()
