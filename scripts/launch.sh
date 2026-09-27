#!/usr/bin/env bash
# Open an app on the bike by nickname or package name.
#
#   scripts/launch.sh netflix
#   scripts/launch.sh com.example.app
set -euo pipefail
source "$(dirname "$0")/common.sh"

case "${1:-}" in
    "")       echo "usage: $0 <pelo|peloton|netflix|newpipe|grupetto|package.name>" >&2; exit 1 ;;
    pelo)     pkg=$PELO_APP ;;
    peloton)  pkg=com.peloton.launcher ;;
    netflix)  pkg=com.netflix.mediaclient ;;
    newpipe)  pkg=org.schabi.newpipe ;;
    grupetto) pkg=com.spop.poverlay ;;
    *)        pkg=$1 ;;
esac

require_device
adb shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 \
    || { echo "Couldn't launch $pkg (not installed?)" >&2; exit 1; }
echo "Launched $pkg"
