#!/usr/bin/env python3
"""Capture one locale's raw screenshots on an Android device.
usage: android_capture.py pixel|tablet LOCALE [part...]   parts: record overview tones"""
import json, os, re, subprocess, sys, time, shutil
DEV = {
 "pixel": dict(id="emulator-5556", name="pixel7a", rec_tab="point:270,2272", ov_tab="point:675,2272",
     K={'7':(162,1550),'8':(350,1551),'9':(539,1551),'4':(162,1728),'5':(350,1729),'6':(539,1729),'1':(162,1906),'2':(350,1907),'3':(539,1907),'0':(350,2085),'.':(162,2084)},
     AC=(917,1729), scrim=(540,1500), title=(400,192), blank=None, toggle=(540,517), mode=(891,192), pager=(900,1786,200,1786), pencil=(746,2094),
     dlg_cancel=(430,1773), discard=(650,1289)),
 "tablet": dict(id="emulator-5554", name="medium-tablet", rec_tab="text:record", ov_tab="text:overview",
     K={'7':(1322,648),'8':(1585,648),'9':(1848,648),'4':(1322,904),'5':(1585,904),'6':(1848,904),'1':(1322,1160),'2':(1585,1160),'3':(1848,1160),'0':(1585,1416),'.':(1321,1416)},
     AC=(2376,904), scrim=(1280,1450), title=(1000,122), blank=(700,1350), toggle=(815,398), mode=(2373,122), pager=(2350,920,1550,920), pencil=(2426,1188),
     dlg_cancel=None, discard=(1372,870)),
}
dev, loc = sys.argv[1], sys.argv[2]; parts = sys.argv[3:] or ["record", "overview", "tones"]
C = DEV[dev]; D = C["id"]
L = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data", "locales.json")))[loc]; U = L["ui"]
B = os.environ.get("STORE_SHOTS_BRIDGE") or os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "..", "build", "screenshot-bridge")
O = f"{B}/files/originals/android/{C['name']}/{loc}"
def sh(*a): return subprocess.run(list(a), capture_output=True, text=True).stdout
def adb(*a): sh(os.path.join(os.path.dirname(os.path.abspath(__file__)), "br.py"), "--", "adb", "-s", D, *a)
def flow(y): r = subprocess.run([os.path.join(os.path.dirname(os.path.abspath(__file__)), "m.sh"), D], input=y, text=True, capture_output=True, env=dict(os.environ, TAILN="1")); return r.stdout.strip()
def cap(n): print(sh(os.path.join(os.path.dirname(os.path.abspath(__file__)), "cap.sh"), D, "android", C["name"], loc, n).strip().splitlines()[-1], flush=True)
def ui():
    out = []
    for line in sh(os.path.join(os.path.dirname(os.path.abspath(__file__)), "ui.py"), D).splitlines():
        m = re.match(r"'(.*)'\s+@\((-?\d+),(-?\d+)\)", line)
        if m: out.append((m.group(1), int(m.group(2)), int(m.group(3))))
    return out
def pos(t):
    for s, x, y in ui():
        if s == t: return x, y
def q(s): return json.dumps(s, ensure_ascii=False)
def pt(x, y): return f'- tapOn:\n    point: "{x},{y}"\n'
def nav(spec):
    k, v = spec.split(":", 1)
    return pt(*map(int, v.split(","))) if k == "point" else f'- tapOn: {q(U[v])}\n'
def unfocus(): return "- hideKeyboard\n" + (pt(*C["blank"]) if C["blank"] else "")
def clear_entry(note_xy):
    adb("shell", "input", "tap", *map(str, C["AC"]))
    flow(f'- tapOn: {q(L["cats"][0])}\n')
    adb("shell", "input", "tap", *map(str, note_xy)); time.sleep(.8)
    adb("shell", "input", "keyevent", "123"); adb("shell", "input", "keyevent", *(["67"] * 25))
    flow("- hideKeyboard\n" + (pt(*C["blank"]) if C["blank"] else ""))
    time.sleep(.8)
def entry(book, note, amt):
    flow(nav(C["rec_tab"]) + f'- tapOn: {q(U["select_book"])}\n- tapOn: {q(book)}\n- waitForAnimationToEnd\n')
    note_xy = pos(U["ph_exp"])
    y = f'- tapOn: {q(L["cats"][0])}\n' + pt(*note_xy) + f'- inputText: {q(note)}\n' + unfocus()
    for ch in amt: y += pt(*C["K"][ch])
    y += (pt(*C["blank"]) if C["blank"] else "") + "- waitForAnimationToEnd\n"   # never hideKeyboard here: it presses Back
    flow(y); return note_xy
anchors_src = f"{B}/files/originals/android/{C['name']}/zh-Hant/anchors.json"
os.makedirs(O, exist_ok=True)

if "currency" in parts:
    flow(nav(C["rec_tab"]) + f'- tapOn: {q(U["settings"])}\n- tapOn: {q(U["pref_currency"])}\n- waitForAnimationToEnd\n')
    sx, sy = [(x, y) for s, x, y in ui() if " or " in s or "或" in s or "または" in s or "또는" in s or s.endswith(L["cur"]) or "USD" in s or "TWD" in s][0]
    flow(pt(sx, sy) + f'- inputText: {q(L["cur"])}\n- hideKeyboard\n')
    res = [(s, x, y) for s, x, y in ui() if y > sy + 150]
    flow(pt(res[0][1], res[0][2]) + f'- tapOn:\n    text: {q(U["confirm"])}\n    optional: true\n- waitForAnimationToEnd\n- tapOn:\n    text: {q(U["back"])}\n    optional: true\n')
    print("preferred currency set", res[:2])

if "record" in parts:
    adb("shell", "am", "force-stop", "com.kevlina.budgetplus.stage")   # resets the record date to today
    adb("shell", "am", "start", "-n", "com.kevlina.budgetplus.stage/com.kevlina.budgetplus.book.BookActivity"); time.sleep(4)
    flow(nav(C["rec_tab"]) + "- waitForAnimationToEnd\n")
    flow(f'- tapOn: {q(U["select_book"])}\n- tapOn: {q(L["main"])}\n- waitForAnimationToEnd\n- tapOn: {q(U["select_book"])}\n- waitForAnimationToEnd\n')
    cap("book-menu"); adb("shell", "input", "tap", *map(str, C["scrim"])); time.sleep(1)
    if "skipmenu" in parts: pass
    nxy = entry(L["main"], *L["entry"]); cap("record-entry"); clear_entry(nxy)
    nxy = entry(L["trip"], *L["trip_entry"])
    # conversion row: the right-aligned converted amount just below the amount field
    conv = [(s, x, y) for s, x, y in ui() if (x > 900 if dev == "pixel" else 900 < x < 1200) and nxy[1] + 120 < y < nxy[1] + 420 and re.search(r"\d", s)]
    cap("currency-record")
    a = json.load(open(anchors_src)); 
    if conv: a["currency-record"] = {"convert": [conv[-1][1] - 150, conv[-1][2]]}
    json.dump(a, open(f"{O}/anchors.json", "w"), indent=2); print("conv", conv[-1:] if conv else None)
    clear_entry(nxy)

if "overview" in parts:
    flow(f'- tapOn: {q(U["select_book"])}\n- tapOn: {q(L["trip"])}\n- waitForAnimationToEnd\n' + nav(C["ov_tab"]) + "- waitForAnimationToEnd\n")
    time.sleep(1.5)
    syms = [(t, x, y) for t, x, y in ui() if len(t) <= 4 and not re.search(r"\d", t) and re.search(r"[¥$₩￥]", t)]
    tg = C["toggle"]
    for a in syms:
        for b in syms:
            if a is not b and abs(a[2] - b[2]) < 10 and 60 < b[1] - a[1] < 400: tg = ((a[1] + b[1]) // 2, a[2])
    print("toggle", tg)
    cap("currency-overview-jpy"); adb("shell", "input", "tap", *map(str, tg)); time.sleep(1.5)
    cap("currency-overview-twd"); adb("shell", "input", "tap", *map(str, tg)); time.sleep(1)
    flow(nav(C["rec_tab"]) + f'- tapOn: {q(U["select_book"])}\n- tapOn: {q(L["main"])}\n- waitForAnimationToEnd\n' + nav(C["ov_tab"]) + "- waitForAnimationToEnd\n")
    for i in range(2):
        summary = any(s.endswith("%") for s, x, y in ui())
        cap("overview-summary" if summary else "overview-list")
        adb("shell", "input", "tap", *map(str, C["mode"])); time.sleep(2)

if "tones" in parts:
    flow(nav(C["rec_tab"]) + f'- tapOn: {q(U["settings"])}\n- scrollUntilVisible:\n    element:\n      text: {q(U["tone_picker"])}\n- tapOn: {q(U["tone_picker"])}\n- waitForAnimationToEnd\n')
    for n in ["tone-sunset", "tone-field"]:
        adb("shell", "input", "swipe", *map(str, C["pager"]), "250"); time.sleep(1.5); cap(n)
    adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", "budgetplus://colors?hex=fff9fafb%3bfff9e7ef%3bffe59ebf%3bff849faf", "com.kevlina.budgetplus.stage")
    time.sleep(3); adb("shell", "input", "tap", *map(str, C["title"])); time.sleep(5)   # dismiss share tooltip by tapping the toolbar title; wait for snackbar
    cap("tone-share")
    adb("shell", "input", "tap", *map(str, C["pencil"])); time.sleep(1.5); cap("color-wheel")
    flow(f'- tapOn: {q(U["cancel"])}\n- tapOn: {q(U["back"])}\n- tapOn:\n    text: {q(U["confirm"])}\n    optional: true\n- waitForAnimationToEnd\n'
         f'- tapOn:\n    text: {q(U["back"])}\n    optional: true\n')
if "tones" in parts:   # make sure we leave the picker without saving
    for _ in range(3):
        u = ui(); names = [t for t, x, y in u]
        if U["tone_picker"] not in names and not any(len(t) > 8 and t.rstrip()[-1:] in "?？" for t in names): break
        cf = [(x, y) for t, x, y in u if t == U["confirm"]]
        if cf: adb("shell", "input", "tap", *map(str, cf[0]))
        else:
            bk = [(x, y) for t, x, y in u if t == U["back"]]
            if bk: adb("shell", "input", "tap", *map(str, bk[0]))
        time.sleep(1.5)
print("done", dev, loc, parts)
