#!/usr/bin/env python3
"""Capture one locale's raw screenshots on an iOS simulator.
usage: ios_capture.py iphone|duo|ipad LOCALE [part...]   parts: currency record overview tones"""
import json, os, re, subprocess, sys, time
KP = lambda r: dict(zip("789456123.0", r))
DEV = {
 "iphone": dict(id="BC8979A0-03EB-491F-980D-CC358CEEB16E", name="iphone18pro", disp=None, s=3, rec=(100,815), ov=(301,815),
    K=KP([(61,547),(131,547),(201,547),(61,614),(131,614),(201,614),(61,680),(131,680),(201,680),(61,745),(131,745)]), blank=(300,480), scrim=(300,650), pencil=(277,747), fix=False),
 "duo": dict(id="6661B9AB-935D-486A-8F9B-1C30821B7B66", name="iphone-duo-outer", disp="1", s=3, rec="record", ov="overview",
    K=KP([(59,449),(125,449),(191,449),(59,511),(125,511),(191,511),(59,573),(125,573),(191,573),(59,635),(125,635)]), blank=(250,420), scrim=(250,600), pencil=(261,636), fix=True),
 "ipad": dict(id="60EE8250-6394-4778-A04E-0463D91F1E1E", name="ipad-13", disp=None, s=2, rec=(256,1308), ov=(766,1308),
    K=KP([(260,838),(386,838),(512,838),(260,960),(386,960),(512,960),(260,1081),(386,1081),(512,1081),(260,1203),(386,1203)]), blank=(700,600), scrim=(700,600), pencil=(654,1232), fix=False),
}
dev, loc = sys.argv[1], sys.argv[2]; parts = sys.argv[3:] or ["record", "overview", "modes", "tones"]
C = DEV[dev]; D = C["id"]; P = "com.kevlina.budgetplus.stage"
L = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data", "locales.json")))[loc]; U = L["ui"]
B = os.environ.get("STORE_SHOTS_BRIDGE") or os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "..", "build", "screenshot-bridge")
O = f"{B}/files/originals/ios/{C['name']}/{loc}"; os.makedirs(O, exist_ok=True)
def sh(*a, env=None): return subprocess.run(list(a), capture_output=True, text=True, env=env).stdout
def br(*a): return sh(os.path.join(os.path.dirname(os.path.abspath(__file__)), "br.py"), "--", *a)
def flow(y):
    r = subprocess.run([os.path.join(os.path.dirname(os.path.abspath(__file__)), "m.sh"), D], input=y, text=True, capture_output=True, env=dict(os.environ, TAILN="1")).stdout.strip(); print("   >", r[-120:], flush=True); return r
def cap(n):
    env = dict(os.environ); 
    if C["disp"]: env["SIM_DISPLAY"] = C["disp"]
    print(sh(os.path.join(os.path.dirname(os.path.abspath(__file__)), "cap.sh"), D, "ios", C["name"], loc, n, env=env).strip().splitlines()[-1], flush=True)
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
def rx(s): return q(re.escape(s))
def pt(x, y): return f'- tapOn:\n    point: "{x},{y}"\n'
def nav(v): return pt(*v) if isinstance(v, tuple) else f'- tapOn: {q(U[v])}\n'
def launch():
    br("xcrun", "simctl", "launch", "--terminate-running-process", D, P, "-AppleLanguages", f"({L['ios_lang']})", "-AppleLocale", L["ios_locale"]); time.sleep(5)
def book(name): return f'- waitForAnimationToEnd\n- tapOn: {q(U["select_book"])}\n- tapOn: {q(name)}\n- waitForAnimationToEnd\n'
TIPS = ["You can choose", "Export", "Tap to", "Tap here", "你可以", "您可以", "輸出", "输出", "點擊", "点击", "カテゴリ", "タップ", "エクスポート", "선택", "탭하", "내보내"]
def dismiss_tips():
    for _ in range(4):
        u = ui()
        if not any(any(t in s for t in TIPS) and len(s) > 8 for s, x, y in u): return
        flow(pt(*{"iphone": (120, 90), "duo": (100, 45), "ipad": (300, 68)}[dev]))   # toolbar title: safe to tap
FIX = '- swipe:\n    start: "200,100"\n    end: "200,395"\n    duration: 600\n' * 2 if C["fix"] else ""
def entry(bk, note, amt):
    launch(); flow(book(bk) + f'- tapOn: {q(L["cats"][0])}\n')
    nx, ny = pos(U["ph_exp"])
    y = pt(nx, ny) + "- waitForAnimationToEnd\n" + f'- inputText: {q(note)}\n' + pt(*C["blank"])
    for ch in amt: y += pt(*C["K"][ch])
    flow(y + FIX + "- waitForAnimationToEnd\n")

if "currency" in parts:
    launch(); flow(f'- tapOn: {q(U["settings"])}\n- tapOn: {q(U["pref_currency"])}\n- waitForAnimationToEnd\n')
    u = ui(); sx, sy = [(x, y) for s, x, y in u if re.search(r" or |或|または|또는", s)][0]
    flow(pt(sx, sy) + f'- inputText: {q(L.get("cur_names", {}).get(L["cur"], L["cur"]))}\n')
    res = [(s, x, y) for s, x, y in ui() if y > sy + 30 and s.strip() and not re.search(r"bars|Wi-Fi|charging|PM|AM|Stage|極簡|极简|ミニマル|미니멀", s)]
    flow(pt(res[0][1], res[0][2]) + f'- tapOn:\n    text: {q(U["confirm"])}\n    optional: true\n- waitForAnimationToEnd\n- tapOn:\n    text: {q(U["back"])}\n    optional: true\n')
    print("preferred", res[:2])

if "record" in parts and "trip-only" not in os.environ.get("REC", ""):
    launch(); flow(book(L["main"]) + f'- tapOn: {q(U["select_book"])}\n- waitForAnimationToEnd\n'); cap("book-menu")
    entry(L["main"], *L["entry"]); cap("record-entry")
if "record" in parts:
    entry(L["trip"], *L["trip_entry"]); time.sleep(4)   # let exchange rates load
    if C["fix"] and not any(re.search(r"\d\.\d\d$", t) and y < 400 for t, x, y in ui()):  # Duo outer: nudge only if the conversion row is hidden
        flow('- swipe:\n    start: "200,380"\n    end: "200,300"\n    duration: 800\n- waitForAnimationToEnd\n')
    nx, ny = pos(L["cats"][0]) or (0, 0)
    conv = [(s, x, y) for s, x, y in ui() if re.search(r"\d", s) and x > 200 and not re.search(r"bars|PM|AM|^\d$|Nov|11月|11월|20\d\d|:", s) and y < 700]
    cap("currency-record")
    a = json.load(open(f"{B}/files/originals/ios/{C['name']}/zh-Hant/anchors.json"))
    if conv:
        s_, x_, y_ = max(conv, key=lambda t: t[2]); a["currency-record"] = {"convert": [x_ * C["s"] - 150, y_ * C["s"]]}
    json.dump(a, open(f"{O}/anchors.json", "w"), indent=2); print("conv", conv[-2:])

if "overview" in parts:
    launch(); flow(book(L["trip"]) + nav(C["ov"]) + "- waitForAnimationToEnd\n"); dismiss_tips()
    cap("currency-overview-jpy")
    tog = [s for s, x, y in ui() if re.fullmatch(r"\S{1,3}, \S{1,3}", s)]
    t = f'- tapOn:\n    text: {rx(tog[0])}\n' if tog else ""
    flow(t + "- waitForAnimationToEnd\n"); cap("currency-overview-twd"); flow(t)
if "modes" in parts:
    launch(); flow(book(L["main"]) + nav(C["ov"]) + "- waitForAnimationToEnd\n"); dismiss_tips()
    u = ui(); summary = any(s.endswith("%") for s, x, y in u)
    first, second = ("overview-summary", "overview-list") if summary else ("overview-list", "overview-summary")
    if "only" not in os.environ.get("MODES", "") or first in os.environ["MODES"]: cap(first)
    flow(pt(*{"iphone": (330, 90), "duo": (424, 228), "ipad": (930, 68)}[dev]) + "- waitForAnimationToEnd\n")
    cap(second)

if "tones" in parts:
    launch(); flow(f'- tapOn: {q(U["settings"])}\n- scrollUntilVisible:\n    element:\n      text: {q(U["tone_picker"])}\n- tapOn: {q(U["tone_picker"])}\n- waitForAnimationToEnd\n')
    px, py = pos("color_tone_pager"); w = {"iphone": 150, "duo": 150, "ipad": 270}[dev]
    for n in ["tone-sunset", "tone-field"]:
        flow(f'- swipe:\n    start: "{px + w},{py}"\n    end: "{px - w},{py}"\n    duration: 400\n- waitForAnimationToEnd\n'); cap(n)
    br("xcrun", "simctl", "openurl", D, "budgetplus://colors?hex=fff9fafb%3bfff9e7ef%3bffe59ebf%3bff849faf"); time.sleep(3)
    flow('- tapOn:\n    text: "Open"\n    optional: true\n- waitForAnimationToEnd\n'); time.sleep(1)
    for _ in range(2):
        if any(len(s) > 8 and any(t in s for t in TIPS + ["share", "分享", "共有", "공유"]) and y < 300 for s, x, y in ui()):
            flow(pt(*C["scrim"]))
    time.sleep(4); cap("tone-share")
    flow(pt(*C["pencil"]) + "- waitForAnimationToEnd\n"); time.sleep(4); cap("color-wheel")   # let the input-mode indicator fade
    flow(f'- tapOn: {q(U["cancel"])}\n- tapOn: {q(U["back"])}\n- tapOn:\n    text: {q(U["confirm"])}\n    optional: true\n')
print("done", dev, loc, parts)
