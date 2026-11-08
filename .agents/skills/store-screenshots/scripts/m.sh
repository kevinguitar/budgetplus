#!/bin/bash
# Run a Maestro flow built from stdin on one device, via the bridge.
# usage: m.sh DEVICE [APPID] < steps.yaml
HERE=$(cd "$(dirname "$0")" && pwd)
BRIDGE=${STORE_SHOTS_BRIDGE:-$HERE/../../../../build/screenshot-bridge}
D=$1; APP=${2:-com.kevlina.budgetplus.stage}
mkdir -p "$BRIDGE/flows"; N=dyn-$D-$RANDOM.yml
{ echo "appId: $APP"; echo "---"; cat; } > "$BRIDGE/flows/$N"
"$HERE/br.py" --timeout 170 -- maestro --device "$D" test "flows/$N" 2>&1 | grep -E "FAILED|COMPLETED|Error|error" | grep -v WARN | tail -${TAILN:-3}
