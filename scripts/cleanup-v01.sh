#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
rm -f "$ROOT/app/src/main/java/com/bridgeconn/soundalert/AppBus.kt"
rm -f "$ROOT/app/src/main/java/com/bridgeconn/soundalert/audio/HeuristicSoundClassifier.kt"
rm -f "$ROOT/app/src/main/java/com/bridgeconn/soundalert/ui/Theme.kt"
rm -f "$ROOT/app/src/test/java/com/bridgeconn/soundalert/audio/HeuristicSoundClassifierTest.kt"
echo "Legacy v0.1 sources removed."
