#!/bin/bash
L=$1; W=$(dirname "$0")/compose.py; STORE_SHOTS_BRIDGE=${STORE_SHOTS_BRIDGE:-$(dirname "$0")/../../../../build/screenshot-bridge}; B=$STORE_SHOTS_BRIDGE/files; O=$B/originals
r(){ n=$(python3 $W --originals $O/$1/$L --out $B/final/$2/$L --locale $L --size $3 | grep -c png); echo "$2 $n"; }
r android/pixel7a android/phone 1440x2560; r android/medium-tablet android/tablet 2560x1440; r ios/iphone18pro ios/iphone-6.3 1206x2622; r ios/iphone-duo-outer ios/iphone-duo-outer 1398x2034; r ios/ipad-13 ios/ipad-13 2048x2732
cd $B; python3 - "$L" <<'PY'
import sys, glob
from PIL import Image
L=sys.argv[1]; rows=[('android/phone',(270,480)),('android/tablet',(480,270)),('ios/iphone-6.3',(221,480)),('ios/iphone-duo-outer',(330,480)),('ios/ipad-13',(360,480))]
H=sum(sz[1]+20 for _,sz in rows)+20; s=Image.new('RGB',(8*490,H),(250,250,250)); y=20
for d,sz in rows:
    x=20
    for f in sorted(glob.glob(f'final/{d}/{L}/*.png')):
        s.paste(Image.open(f).convert('RGB').resize(sz),(x,y)); x+=sz[0]+12
    y+=sz[1]+20
s.save(f'review-all-{L}.png', optimize=True); print('sheet', s.size)
PY
