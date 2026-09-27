#!/usr/bin/env bash
# Make Pelo the bike's home screen (what the Home / "P" button opens).
# Undo with scripts/restore.sh, or tap "Peloton" on Pelo's home screen.
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_device

if ! is_installed "$PELO_APP"; then
    echo "$PELO_APP isn't installed. Run scripts/deploy.sh first." >&2
    exit 1
fi

adb shell cmd package set-home-activity "$PELO_HOME" >/dev/null
adb shell input keyevent KEYCODE_HOME
echo "Home screen is now $PELO_HOME"
