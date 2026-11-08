#!/usr/bin/env python3
"""Screenshot bridge for Claude.

usage: python3 .agents/skills/store-screenshots/scripts/bridge.py [WORKDIR]
WORKDIR defaults to <repo>/build/screenshot-bridge (gitignored).

Watches WORKDIR/queue for job files and runs a small allowlist of commands:
  adb ..., xcrun simctl ..., maestro ..., sleep N
Commands are run directly (no shell). Output files may only be written
inside this folder. Stop any time with Ctrl-C.
"""
import json, os, shutil, subprocess, sys, time
from pathlib import Path

ROOT = (Path(sys.argv[1]) if len(sys.argv) > 1 else
        Path(__file__).resolve().parents[4] / "build" / "screenshot-bridge").resolve()
ROOT.mkdir(parents=True, exist_ok=True)
QUEUE, RESULTS = ROOT / "queue", ROOT / "results"

def find(name, extra):
    p = shutil.which(name)
    if p: return p
    for c in extra:
        c = os.path.expanduser(c)
        if os.path.exists(c): return c
    return None

TOOLS = {
    "adb": find("adb", [os.environ.get("ANDROID_HOME", "") + "/platform-tools/adb",
                        "~/Library/Android/sdk/platform-tools/adb"]),
    "xcrun": find("xcrun", ["/usr/bin/xcrun"]),
    "maestro": find("maestro", ["~/.maestro/bin/maestro"]),
    "sleep": find("sleep", ["/bin/sleep"]),
}

def inside(path):
    p = (ROOT / path).resolve()
    if ROOT not in p.parents and p != ROOT:
        raise ValueError(f"path outside bridge folder: {path}")
    return p

def run(job):
    argv = job["argv"]
    tool = argv[0]
    if tool not in TOOLS or not TOOLS[tool]:
        raise ValueError(f"command not allowed or not found: {tool}")
    if tool == "xcrun" and (len(argv) < 2 or argv[1] != "simctl"):
        raise ValueError("only 'xcrun simctl' is allowed")
    out_path = inside(job["stdout"]) if job.get("stdout") else None
    if out_path: out_path.parent.mkdir(parents=True, exist_ok=True)
    env = dict(os.environ)
    env.update(job.get("env", {}))
    r = subprocess.run([TOOLS[tool]] + argv[1:], cwd=ROOT, env=env,
                       capture_output=True, timeout=job.get("timeout", 300))
    if out_path:
        out_path.write_bytes(r.stdout)
        stdout = f"<{len(r.stdout)} bytes written to {job['stdout']}>"
    else:
        stdout = r.stdout.decode("utf-8", "replace")[-20000:]
    return {"rc": r.returncode, "stdout": stdout,
            "stderr": r.stderr.decode("utf-8", "replace")[-20000:]}

def main():
    QUEUE.mkdir(exist_ok=True); RESULTS.mkdir(exist_ok=True)
    print("Screenshot bridge running in", ROOT)
    for k, v in TOOLS.items(): print(f"  {k}: {v or 'NOT FOUND'}")
    print("Press Ctrl-C to stop.\n")
    while True:
        for f in sorted(QUEUE.glob("*.json")):
            try:
                job = json.loads(f.read_text())
                print(time.strftime("%H:%M:%S"), " ".join(job["argv"])[:160], flush=True)
                res = run(job)
            except Exception as e:
                res = {"rc": -1, "stdout": "", "stderr": f"bridge error: {e}"}
            tmp = RESULTS / (f.stem + ".tmp")
            tmp.write_text(json.dumps(res))
            tmp.rename(RESULTS / (f.stem + ".json"))
            f.unlink()
        time.sleep(0.3)

if __name__ == "__main__":
    try: main()
    except KeyboardInterrupt: print("\nBridge stopped.")
