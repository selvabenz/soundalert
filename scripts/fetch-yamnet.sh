#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/app/src/main/assets/yamnet.tflite"
URL='https://tfhub.dev/google/lite-model/yamnet/classification/tflite/1?lite-format=tflite'
SHA='10c95ea3eb9a7bb4cb8bddf6feb023250381008177ac162ce169694d05c317de'
mkdir -p "$(dirname "$OUT")"
curl --fail --location --retry 3 "$URL" -o "$OUT"
echo "$SHA  $OUT" | sha256sum -c -
echo "YAMNet ready: $OUT"
