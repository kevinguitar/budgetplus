#!/usr/bin/env python3
"""Enter records on the Pixel 7a record screen.
usage: rec.py <device> <dayRegexTemplate> <confirmLabel>  < records.txt
records.txt lines: E|I ; category ; note ; amount ; day
Coordinates are for Pixel 7a (1080x2400) record screen with 8 categories."""
import sys, subprocess, os
dev, dayfmt, confirm = sys.argv[1:4]
K={'7':(162,1550),'8':(350,1551),'9':(539,1551),'4':(162,1728),'5':(350,1729),'6':(539,1729),
   '1':(162,1906),'2':(350,1907),'3':(539,1907),'0':(350,2085),'.':(162,2084)}
OK=(917,1996); DATE=(162,1126); NOTE=(597,937); EXP=(289,370); INC=(790,370)
def pt(p): return f'- tapOn:\n    point: "{p[0]},{p[1]}"\n'
lines=[l.strip() for l in sys.stdin if l.strip() and not l.startswith('#')]
for chunk in [lines[i:i+5] for i in range(0,len(lines),5)]:
    y=""
    for l in chunk:
        t,cat,note,amt,day=[x.strip() for x in l.split(';')]
        y+=pt(INC if t=='I' else EXP)
        y+=f'- tapOn: "{cat}"\n'
        n,d=((597,811),(162,1000)) if t=='I' else (NOTE,DATE)
        y+=pt(n)+f'- inputText: "{note}"\n- hideKeyboard\n'
        y+=pt(d)+f'- tapOn:\n    text: "{dayfmt.format(d=day)}"\n- tapOn: "{confirm}"\n'
        for ch in amt: y+=pt(K[ch])
        y+=pt(OK)+'- waitForAnimationToEnd\n'
    r=subprocess.run([os.path.join(os.path.dirname(os.path.abspath(__file__)),'m.sh'),dev],input=y,text=True,capture_output=True,env=dict(os.environ,TAILN="2"))
    print(chunk[0][:30],'...',r.stdout.strip().replace('\n',' | '))
