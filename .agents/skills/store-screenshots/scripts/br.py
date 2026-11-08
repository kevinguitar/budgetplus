#!/usr/bin/env python3
# Client side of bridge.py: queue one job and print its output.
# usage: br.py [--out rel/path] [--timeout N] -- argv...
import json, os, sys, time, uuid
from pathlib import Path
B = Path(os.environ.get("STORE_SHOTS_BRIDGE") or os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "..", "build", "screenshot-bridge")).resolve()
a = sys.argv[1:]; job = {}
while a and a[0] != "--":
    if a[0] == "--out": job["stdout"] = a[1]; a = a[2:]
    elif a[0] == "--timeout": job["timeout"] = int(a[1]); a = a[2:]
    else: sys.exit("bad arg " + a[0])
job["argv"] = a[1:]
jid = time.strftime("%Y%m%d%H%M%S") + "-" + uuid.uuid4().hex[:6]
tmp = B / "queue" / (jid + ".tmp"); tmp.write_text(json.dumps(job)); tmp.rename(B / "queue" / (jid + ".json"))
res = B / "results" / (jid + ".json"); t0 = time.time()
lim = job.get("timeout", 300) + 60
while not res.exists():
    if time.time() - t0 > lim: sys.exit("timeout waiting for bridge (is it running?)")
    time.sleep(0.3)
r = json.loads(res.read_text())
sys.stdout.write(r["stdout"]); sys.stderr.write(r["stderr"]); sys.exit(r["rc"] if r["rc"] >= 0 else 99)
