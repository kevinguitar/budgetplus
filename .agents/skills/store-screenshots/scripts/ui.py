#!/usr/bin/env python3
# usage: ui.py <device-id>   -> prints visible texts with center coords
import json,os,subprocess,sys,re
HERE=os.path.dirname(os.path.abspath(__file__))
BRIDGE=os.environ.get("STORE_SHOTS_BRIDGE") or os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "..", "build", "screenshot-bridge")
d=sys.argv[1]; rel=f"files/tmp/h-{d}.json"
subprocess.run([os.path.join(HERE,"br.py"),"--timeout","90","--out",rel,"--","maestro","--device",d,"hierarchy"],check=True,stdout=subprocess.DEVNULL)
raw=open(os.path.join(BRIDGE,rel)).read()
raw=raw[raw.index("{"):]
h=json.loads(raw)
def walk(n):
  a=n.get('attributes',{})
  t=a.get('text') or a.get('accessibilityText') or a.get('hintText')
  b=a.get('bounds','')
  m=re.findall(r'-?\d+',b)
  if t and len(m)==4 and "inputmethod" not in a.get("resource-id",""):
    x1,y1,x2,y2=map(int,m); print(f"{t!r:45} @({(x1+x2)//2},{(y1+y2)//2}) {a.get('resource-id','')}")
  for c in n.get('children',[]): walk(c)
walk(h)
