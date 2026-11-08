#!/usr/bin/env python3
"""Compose store screenshots from raw device captures.

usage: compose.py --originals DIR --out DIR --locale zh-Hant --size 1440x2560
                  [--frame none|phone|tablet] [--only 03-shared,...]
                  [--captions FILE] [--fonts DIR]

Raw captures are never modified. Captions default to ../captions.json (the skill folder).
Fonts default to build/screenshot-bridge/fonts (run fetch_fonts.py once).
"""
import argparse, json, os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
SKILL = os.path.dirname(HERE)
REPO = os.path.abspath(os.path.join(SKILL, "..", "..", ".."))
FONTS = os.environ.get("STORE_SHOTS_FONTS", os.path.join(REPO, "build", "screenshot-bridge", "fonts"))
OLIVE, CREAM = (0x8A, 0x8B, 0x7D), (0xFF, 0xF2, 0xE0)
BROWN, PINK = (0x95, 0x76, 0x60), (0xE5, 0x9E, 0xBF)  # PINK = Barbie tone primary
FACE = {"zh-Hant": 3, "zh-Hans": 2, "ja": 0, "ko": 1, "en-US": 0}
ICON = {"group_add": "", "share": "", "currency_exchange": "",
        "bar_chart": "", "list": "", "colorize": ""}

# Each screen: list of shots (file, x-center, top, width as fraction of canvas W) back-to-front,
# optional crops (file, crop box as fractions of the shot, placement) and an optional badge.
# Each screen: shots (file, x-center, top, width as fraction of canvas W) back-to-front,
# optional crops (file, anchors.json key of a pixel crop box, x-center, top (unused: cards stack from the bottom), width) and an optional badge
# (icon, color, shot file, anchor key[, radius as fraction of the short side]). The badge is centred on that in-app icon, read from
# anchors.json in the originals folder, so it always covers the icon it enlarges.
# Use "float" (icon, color, x, y, size) instead when a badge would hide important UI text.
LAYOUTS = {
  "01-login":    {"shots": [("login", .50, .19, .70)]},
  "02-books":    {"shots": [("book-menu", .50, .19, .70)]},
  "03-shared":   {"shots": [("record-entry", .50, .19, .70)],
                  "badge": ("group_add", BROWN, "record-entry", "invite")},
  "04-currency": {"shots": [("currency-record", .52, .19, .60)],
                  "crops": [("currency-overview-jpy", "crop-currency-overview", .50, .595, .74),
                            ("currency-overview-twd", "crop-currency-overview", .50, .795, .74)],
                  "float": ("currency_exchange", BROWN, .12, .40, .16)},
  "05-overview": {"shots": [("overview-list", .34, .20, .56), ("overview-summary", .66, .27, .56)],
                  "badge": ("bar_chart", BROWN, "overview-summary", "mode")},
  "06-tones":    {"shots": [("tone-sunset", .34, .20, .56), ("tone-field", .66, .27, .56)]},
  "07-colors":   {"shots": [("color-wheel", .50, .18, .64)],
                  "badge": ("colorize", PINK, "color-wheel", "edit")},
  "08-share":    {"shots": [("tone-share", .50, .19, .70)],
                  "badge": ("share", PINK, "tone-share", "share")},
}


# Landscape canvases (tablets in landscape): same screens, wide raw captures.
LAYOUTS_LANDSCAPE = {
  "01-login":    {"shots": [("login", .50, .21, .70)]},
  "02-books":    {"shots": [("book-menu", .50, .21, .70)]},
  "03-shared":   {"shots": [("record-entry", .50, .21, .70)],
                  "badge": ("group_add", BROWN, "record-entry", "invite")},
  "04-currency": {"shots": [("currency-record", .41, .21, .62)],
                  "crops": [("currency-overview-jpy", "crop-currency-overview", .79, .42, .36),
                            ("currency-overview-twd", "crop-currency-overview", .79, .69, .36)],
                  "float": ("currency_exchange", CREAM, .88, .30, .09)},
  "05-overview": {"shots": [("overview-list", .29, .21, .50), ("overview-summary", .71, .33, .50)],
                  "badge": ("bar_chart", BROWN, "overview-summary", "mode")},
  "06-tones":    {"shots": [("tone-sunset", .29, .21, .50), ("tone-field", .71, .33, .50)]},
  "07-colors":   {"shots": [("color-wheel", .50, .21, .70)],
                  "badge": ("colorize", PINK, "color-wheel", "edit")},
  "08-share":    {"shots": [("tone-share", .50, .21, .70)],
                  "badge": ("share", PINK, "tone-share", "share")},
}

def font(locale, px, bold=False):
    f = os.path.join(FONTS, "NotoSansCJK-%s.ttc" % ("Bold" if bold else "Regular"))
    return ImageFont.truetype(f, px, index=FACE.get(locale, 0))

def rounded(img, r):
    m = Image.new("L", img.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, img.size[0] - 1, img.size[1] - 1), r, fill=255)
    out = img.convert("RGBA"); out.putalpha(m); return out

def shadow(canvas, box, r, blur, alpha=70):
    s = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(s).rounded_rectangle(box, r, fill=(60, 45, 30, alpha))
    canvas.alpha_composite(s.filter(ImageFilter.GaussianBlur(blur)))

def framed(shot, w, frame):
    """Scale shot to width w; optionally wrap in a plain device bezel."""
    h = round(shot.height * w / shot.width)
    shot = shot.convert("RGB").resize((w, h), Image.LANCZOS)
    if frame == "none":
        return rounded(shot, round(w * .055))
    bez = round(w * (.035 if frame == "phone" else .03))
    body = Image.new("RGBA", (w + 2 * bez, h + 2 * bez), (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    d.rounded_rectangle((0, 0, body.width - 1, body.height - 1), round(w * .13 if frame == "phone" else w * .05), fill=(28, 28, 30, 255))
    d.rounded_rectangle((2, 2, body.width - 3, body.height - 3), round(w * .13 if frame == "phone" else w * .05), outline=(90, 90, 95, 255), width=max(2, bez // 6))
    body.alpha_composite(rounded(shot, round(w * (.10 if frame == "phone" else .035))), (bez, bez))
    return body

def caption(canvas, lines, locale, band):
    W = canvas.width
    lines = [l for l in lines if l]
    cjk = locale != "en-US"
    if canvas.width > canvas.height:  # landscape: one line, sized from height
        lines = [("\u3000" if cjk else " ").join(lines)]
        px = round(canvas.height * (.075 if cjk else .07))
    else:  # size from a 9:16-equivalent width so wide canvases (iPad) don't get huge captions
        px = round(min(W, canvas.height * 9 / 16) * (.075 if cjk else .07))
    # shrink until every line fits within 88% width
    while True:
        f = font(locale, px, bold=not cjk)
        sp = round(px * (.18 if cjk else 0))
        widths = [sum(f.getlength(c) for c in l) + sp * (len(l) - 1) for l in lines]
        if max(widths) <= W * .88 or px < 20: break
        px -= 2
    lh = round(px * 1.35)
    y = round(band / 2 - lh * len(lines) / 2)
    d = ImageDraw.Draw(canvas)
    for l, lw in zip(lines, widths):
        x = (W - lw) / 2
        for c in l:
            d.text((x, y), c, font=f, fill=CREAM + (255,)); x += f.getlength(c) + sp
        y += lh

def badge_radius(canvas, rf=.105):
    W = canvas.width
    return round(min(W, canvas.height, max(W, canvas.height) * 9 / 16) * rf)

def badge(canvas, name, color, x, y, rf=.105):
    W = canvas.width; r = badge_radius(canvas, rf)
    x, y = round(x), round(y)
    shadow(canvas, (x - r, y - r + 8, x + r, y + r + 8), r, 18, 50)
    d = ImageDraw.Draw(canvas)
    d.ellipse((x - r, y - r, x + r, y + r), fill=color + (255,))
    f = ImageFont.truetype(os.path.join(FONTS, "mi-round.ttf"), round(r * 1.25))
    d.text((x, y), ICON[name], font=f, fill=CREAM + (255,), anchor="mm")

def compose(screen, originals, locale, size, frame, captions, grow=1.0):
    W, H = size
    cv = Image.new("RGBA", size, OLIVE + (255,))
    landscape = W > H
    # cream background is a true circle whose top touches `arc_top`; a big radius keeps the arc smooth
    R = (1.0 if not landscape else .62) * W
    arc_top = (.21 if not landscape else .30) * H
    ImageDraw.Draw(cv).ellipse((W / 2 - R, arc_top, W / 2 + R, arc_top + 2 * R), fill=CREAM + (255,))
    band = round(H * (.17 if not landscape else .19))
    caption(cv, captions[screen], locale, band)
    L = (LAYOUTS_LANDSCAPE if landscape else LAYOUTS)[screen]
    placed = {}  # shot file -> (x, y, scale) of the screen area on canvas
    # Portrait canvases taller than 9:16 (e.g. iPhone 1206x2622) scale shots up to fill the height.
    k = 1.0 if landscape else max(1.0, (H / W) / (16 / 9))
    if len(L["shots"]) > 1 or L.get("crops"):
        k = min(k, 1.1)  # multi-shot layouts would run off the canvas
    k *= grow
    for name, cx, top, wf in L["shots"]:
        wf = wf * k
        shot = Image.open(os.path.join(originals, name + ".png"))
        img = framed(shot, round(W * wf), frame)
        x, y = round(W * cx - img.width / 2), round(H * top)
        bez = (img.width - round(W * wf)) // 2
        placed[name] = (x + bez, y + bez, round(W * wf) / shot.width)
        if frame == "none":
            shadow(cv, (x, y + 14, x + img.width, y + img.height + 14), round(img.width * .055), 28)
        else:
            shadow(cv, (x, y + 18, x + img.width, y + img.height + 18), round(img.width * .1), 30, 60)
        cv.alpha_composite(img, (x, y))
    # Crop cards stack below the shot's conversion row ("convert" anchor), shrinking the layout until
    # the stack fits; without that anchor they stack up from the bottom edge.
    cards_src = []
    anchors = json.load(open(os.path.join(originals, "anchors.json"))) if os.path.exists(os.path.join(originals, "anchors.json")) else {}
    for name, box, cx, top, wf in L.get("crops", []):
        shot = Image.open(os.path.join(originals, name + ".png")).convert("RGB")
        cards_src.append((shot.crop(tuple(anchors[box])), cx, wf))
    placed_cards = []
    if cards_src:
        main = L["shots"][0][0]
        conv = anchors.get(main, {}).get("convert")
        sx, sy, sk = placed[main]
        def stack(scale):
            out, y = [], None
            cs = [(c.resize((round(W * wf * scale * grow), round(c.height * W * wf * scale * grow / c.width)), Image.LANCZOS), cx) for c, cx, wf in cards_src]
            if conv and not landscape:
                y = sy + conv[1] * sk + H * .03
                for c, cx in cs:
                    out.append((c, cx, round(y))); y += c.height * .985
                return out, y
            y = H * 1.005
            for c, cx in reversed(cs):
                y -= c.height; out.insert(0, (c, cx, round(y))); y += c.height * .015
            return out, H
        scale = 1.0
        while True:
            placed_cards, bottom = stack(scale)
            if bottom <= H * 1.005 or scale < .5: break
            scale -= .04
        if bottom > H * 1.005:  # still too tall: fall back to bottom stacking
            conv = None; placed_cards, _ = stack(scale)
    for c, cx, y in placed_cards:
        w = c.width; x = round(W * cx - w / 2); rad = round(w * .05)
        shadow(cv, (x, y + 12, x + w, y + c.height + 12), rad, 24, 80)
        card = rounded(c, rad)
        ImageDraw.Draw(card).rounded_rectangle((0, 0, w - 1, c.height - 1), rad, outline=BROWN + (255,), width=max(4, w // 120))
        cv.alpha_composite(card, (x, y))
    if "float" in L:  # free-standing glyph beside the phone, not tied to an in-app icon
        icon, color, fx, fy, size = L["float"]
        f = ImageFont.truetype(os.path.join(FONTS, "mi-round.ttf"), round(W * size))
        ImageDraw.Draw(cv).text((W * fx, H * fy), ICON[icon], font=f, fill=color + (255,), anchor="mm")
    if "badge" in L:
        icon, color, shot_name, key, *rad = L["badge"]
        ax, ay = anchors[shot_name][key]
        sx, sy, k = placed[shot_name]
        # Shift the badge up-right as far as it can go while still covering the icon completely.
        ih = anchors.get("icon-half", 32) * k * 1.25  # icon half-size on canvas, with a little margin
        r = badge_radius(cv, *(rad or [.105]))
        off = max(0.0, (r - ih)) / 2 ** .5
        bx, by = sx + ax * k + off, sy + ay * k - off
        bx = min(bx, W - r - W * .015)    # keep the badge on the canvas
        by = max(by, band + r * .9)        # and out of the caption band
        badge(cv, icon, color, bx, by, *rad)
    compose.bottom = max([y + c.height for c, cx, y in placed_cards], default=H)
    return cv.convert("RGB")

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--originals", required=True); ap.add_argument("--out", required=True)
    ap.add_argument("--locale", required=True); ap.add_argument("--size", required=True)
    ap.add_argument("--frame", default="none"); ap.add_argument("--only", default="")
    ap.add_argument("--captions", default=os.path.join(SKILL, "captions.json"))
    ap.add_argument("--fonts", default=None)
    a = ap.parse_args()
    if a.fonts: FONTS = a.fonts
    size = tuple(int(v) for v in a.size.lower().split("x"))
    caps = json.load(open(a.captions))[a.locale]
    os.makedirs(a.out, exist_ok=True)
    for i, screen in enumerate(LAYOUTS, 1):
        if a.only and screen not in a.only.split(","): continue
        L = (LAYOUTS_LANDSCAPE if size[0] > size[1] else LAYOUTS)[screen]
        need = [x[0] for x in L["shots"] + L.get("crops", [])]
        missing = [n for n in need if not os.path.exists(os.path.join(a.originals, n + ".png"))]
        if missing:
            print("skip", screen, "- missing", ", ".join(missing)); continue
        img = compose(screen, a.originals, a.locale, size, a.frame, caps)
        if L.get("crops") and size[1] > size[0]:
            # grow the whole currency layout into leftover space (squarer canvases such as iPad)
            g = 1.0
            max_card = max(c[4] for c in L["crops"])
            while compose.bottom < size[1] * .94 and g < 1.4 and max_card * (g + .04) <= .9:
                trial = compose(screen, a.originals, a.locale, size, a.frame, caps, grow=g + .04)
                if compose.bottom > size[1] * .985: break
                img, g = trial, g + .04
        p = os.path.join(a.out, f"{screen}.png"); img.save(p, optimize=True)
        print(p, img.size)
