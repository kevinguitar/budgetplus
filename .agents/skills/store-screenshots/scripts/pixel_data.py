#!/usr/bin/env python3
"""Prepare one locale's books + records on the Pixel 7a (emulator-5556).
usage: pixel_data.py LOCALE OLD_MAIN OLD_TRIP [step...]   steps: books currency records trip"""
import json, os, subprocess, sys, re
D = "emulator-5556"
L = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data", "locales.json")))[sys.argv[1]]
U = L["ui"]; old = sys.argv[2:4]; steps = sys.argv[4:] or ["books", "currency", "records", "trip"]
M = os.path.join(os.path.dirname(os.path.abspath(__file__)), "m.sh")
def flow(y, tail=2):
    r = subprocess.run([M, D], input=y, text=True, capture_output=True, env=dict(os.environ, TAILN=str(tail)))
    out = r.stdout.strip().replace("\n", " | "); print("  ", out[-200:]); return out
def ui():
    r = subprocess.run([os.path.join(os.path.dirname(os.path.abspath(__file__)), "ui.py"), D], capture_output=True, text=True); return r.stdout
def pos(text):
    for line in ui().splitlines():
        m = re.match(r"'(.*)'\s+@\((\d+),(\d+)\)", line)
        if m and m.group(1) == text: return int(m.group(2)), int(m.group(3))
def pt(x, y): return f'- tapOn:\n    point: "{x},{y}"\n'
def q(s): return json.dumps(s, ensure_ascii=False)
K = {'7':(162,1550),'8':(350,1551),'9':(539,1551),'4':(162,1728),'5':(350,1729),'6':(539,1729),
     '1':(162,1906),'2':(350,1907),'3':(539,1907),'0':(350,2085),'.':(162,2084)}
OK = (917, 1996); EXP = (289, 370); INC = (790, 370)
XS = [162, 288, 414, 540, 666, 792, 918]
FIRST = {"zh-Hans": 6}.get(sys.argv[1], 0)   # column of Nov 1, 2026 (a Sunday); zh-CN weeks start on Monday
def day(d): i = d - 1 + FIRST; return XS[i % 7], 1133 + 126 * (i // 7)

def select_currency(code):
    code = L.get("cur_names", {}).get(code, code)   # CJK keyboards swallow Latin codes; search by name
    return pt(592, 381) + f'- inputText: {q(code)}\n- hideKeyboard\n' + pt(281, 667) + \
        f'- tapOn:\n    text: {q(U["confirm"])}\n    optional: true\n- waitForAnimationToEnd\n'

if "pref" in steps:
    print("preferred currency")
    flow(f'- tapOn:\n    text: {q(U["back"])}\n    optional: true\n- tapOn: {q(U["settings"])}\n- tapOn: {q(U["pref_currency"])}\n- waitForAnimationToEnd\n'
         + select_currency(L["cur"]) + f'- tapOn:\n    text: {q(U["back"])}\n    optional: true\n')
if "books" in steps:
    print("books")
    for name in [L["trip"], L["main"]]:
        flow(f'- tapOn: {q(U["select_book"])}\n- tapOn: {q(U["create_book"])}\n- eraseText: 30\n'
             f'- inputText: {q(name)}\n- hideKeyboard\n- tapOn: {q(U["create"])}\n- waitForAnimationToEnd\n')
    for name in old:
        flow(f'- tapOn:\n    text: {q(U["back"])}\n    optional: true\n- tapOn: {q(U["select_book"])}\n- tapOn: {q(name)}\n- waitForAnimationToEnd\n- tapOn: {q(U["settings"])}\n'
             f'- scrollUntilVisible:\n    element:\n      text: {q(U["delete_book"])}\n- tapOn: {q(U["delete_book"])}\n'
             f'- tapOn: {q(U["confirm"])}\n- waitForAnimationToEnd\n')
if "currency" in steps:
    print("currency")
    # main book currency (new books inherit the preferred one, but make sure), then the trip book
    for book, code in [(L["main"], L["cur"]), (L["trip"], L["trip_cur"])]:
        flow(f'- tapOn:\n    text: {q(U["back"])}\n    optional: true\n- tapOn: {q(U["select_book"])}\n- tapOn: {q(book)}\n- waitForAnimationToEnd\n- tapOn: {q(U["settings"])}\n'
             f'- tapOn: {q(U["book_currency"])}\n' + select_currency(code) +
             f'- tapOn:\n    text: {q(U["back"])}\n    optional: true\n- waitForAnimationToEnd\n')

def enter(book, records):
    flow(f'- tapOn:\n    text: {q(U["back"])}\n    optional: true\n- tapOn: {q(U["select_book"])}\n- tapOn: {q(book)}\n- waitForAnimationToEnd\n')
    # find note/date positions for both tabs
    cache = os.path.join(os.environ.get("STORE_SHOTS_BRIDGE", "/tmp"), "layout-%s.json" % sys.argv[1])
    if os.path.exists(cache):
        lay = {k: tuple(v) for k, v in json.load(open(cache)).items()}
    else:
        lay = {}
        for t, tab in [("E", EXP), ("I", INC)]:
            flow(pt(*tab))
            lay[t] = pos(U["ph_exp"] if t == "E" else U["ph_inc"])
        json.dump(lay, open(cache, "w"))
    print("  layout", lay)
    sk = int(os.environ.get("SKIP", "0")); records = records[sk: sk + int(os.environ.get("LIMIT", "99"))]
    for chunk in [records[i:i+5] for i in range(0, len(records), 5)]:
        y = ""
        for t, ci, note, amt, d in chunk:
            cat = (L["cats"] if t == "E" else L["inc"])[ci]
            nx, ny = lay[t]
            y += pt(*(INC if t == "I" else EXP)) + f'- tapOn: {q(cat)}\n' + pt(nx, ny) + f'- inputText: {q(note)}\n- hideKeyboard\n'
            y += pt(162, ny + 189) + pt(*day(d)) + pt(876, 1886)
            for ch in amt: y += pt(*K[ch])
            y += pt(*OK) + "- waitForAnimationToEnd\n"
        flow(y); print("  done", chunk[-1][2], flush=True)

if "records" in steps:
    print("records main"); enter(L["main"], L["records"])
if "trip" in steps:
    print("records trip"); enter(L["trip"], L["trip_records"])
