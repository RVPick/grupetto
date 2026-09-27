#!/usr/bin/env bash
# Build the app, install it on the bike and launch it.
#
#   scripts/deploy.sh           release build (what you ride with)
#   scripts/deploy.sh --debug   debug build (debuggable, but much slower on the bike)
set -euo pipefail
source "$(dirname "$0")/common.sh"

variant=release
[[ "${1:-}" == "--debug" ]] && variant=debug
task="assemble${variant^}"

require_device

(cd "$PROJECT_DIR" && ./gradlew --quiet ":app:$task")

apk="$PROJECT_DIR/app/build/outputs/apk/$variant/app-$variant.apk"
echo "Installing $(basename "$apk") ($(du -h "$apk" | cut -f1))"
adb install -r "$apk" >/dev/null

"$(dirname "$0")/grant.sh" >/dev/null
adb shell am start -n "$PELO_ACTIVITY" >/dev/null
echo "Launched $PELO_ACTIVITY"
