#!/usr/bin/env bash
# Build the app, install it on the bike and launch it.
#
#   scripts/deploy.sh             debug build
#   scripts/deploy.sh --release   minified release build (closer to real performance)
set -euo pipefail
source "$(dirname "$0")/common.sh"

variant=debug
[[ "${1:-}" == "--release" ]] && variant=release
task="assemble${variant^}"

require_device

(cd "$PROJECT_DIR" && ./gradlew --quiet ":app:$task")

apk="$PROJECT_DIR/app/build/outputs/apk/$variant/app-$variant.apk"
echo "Installing $(basename "$apk") ($(du -h "$apk" | cut -f1))"
adb install -r "$apk" >/dev/null

"$(dirname "$0")/grant.sh" >/dev/null
adb shell am start -n "$PELO_ACTIVITY" >/dev/null
echo "Launched $PELO_ACTIVITY"
