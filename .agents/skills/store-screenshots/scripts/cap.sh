#!/bin/bash
# Save a native-resolution screenshot via the bridge.
# usage: [SIM_DISPLAY=n] cap.sh DEVICE_ID android|ios DEVICE_NAME LOCALE SHOT
#   SIM_DISPLAY picks the display on multi-display simulators (iPhone Duo: see `xcrun simctl io <udid> screenshot` error)
#   -> <bridge>/files/originals/<platform>/<device>/<locale>/<shot>.png
HERE=$(cd "$(dirname "$0")" && pwd)
BRIDGE=${STORE_SHOTS_BRIDGE:-$HERE/../../../../build/screenshot-bridge}
D=$1; P=$2; N=$3; L=$4; S=$5
REL=files/originals/$P/$N/$L/$S.png; mkdir -p "$BRIDGE/$(dirname $REL)"
if [ "$P" = android ]; then "$HERE/br.py" --out $REL -- adb -s $D exec-out screencap -p >/dev/null
else "$HERE/br.py" -- xcrun simctl io $D screenshot --type=png ${SIM_DISPLAY:+--display=$SIM_DISPLAY} "$REL" >/dev/null; fi
python3 -c "from PIL import Image;import sys;print('$REL',Image.open(sys.argv[1]).size)" "$BRIDGE/$REL"
