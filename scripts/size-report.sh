#!/usr/bin/env bash
set -euo pipefail
APK="${1:-app/build/outputs/apk/release/app-release.apk}"
if [[ ! -f "$APK" ]]; then
  echo "Release APK not found: $APK" >&2
  exit 1
fi
BYTES=$(stat -c%s "$APK")
MIB=$(awk "BEGIN {printf \"%.2f\", $BYTES/1048576}")
echo "FocuSense release APK: $BYTES bytes ($MIB MiB)"
if (( BYTES > 30 * 1024 * 1024 )); then
  echo "FAIL: release APK exceeds 30 MiB hard review threshold" >&2
  exit 2
elif (( BYTES > 25 * 1024 * 1024 )); then
  echo "WARN: release APK exceeds 25 MiB target"
fi
