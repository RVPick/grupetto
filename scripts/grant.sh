#!/usr/bin/env bash
# Grant the permissions a sideloaded app can't request through normal dialogs.
set -euo pipefail
source "$(dirname "$0")/common.sh"

require_device

# Draw the ride overlay on top of other apps.
adb shell appops set "$PELO_APP" SYSTEM_ALERT_WINDOW allow
# Install streaming apps from inside the launcher (Phase 5).
adb shell appops set "$PELO_APP" REQUEST_INSTALL_PACKAGES allow
# Scan for Bluetooth heart rate monitors (Android bundles BLE scanning with location).
adb shell pm grant "$PELO_APP" android.permission.ACCESS_FINE_LOCATION
adb shell pm grant "$PELO_APP" android.permission.ACCESS_COARSE_LOCATION
# Read app usage, to list running apps under "Close apps" on the home screen.
adb shell appops set "$PELO_APP" GET_USAGE_STATS allow
# Keep the overlay service alive during long rides.
adb shell dumpsys deviceidle whitelist "+$PELO_APP" >/dev/null

# No mini player (picture-in-picture) for DRM streaming apps: this tablet's display hardware
# can't draw protected video in the small window, so it only shows black. SmartTube, Xtra and
# NewPipe play unprotected video and keep theirs.
for app in com.netflix.mediaclient com.disney.disneyplus com.amazon.avod.thirdpartyclient com.peacocktv.peacockandroid; do
    if is_installed "$app"; then
        adb shell appops set "$app" PICTURE_IN_PICTURE ignore
    fi
done

echo "Granted overlay, install, location, usage access and battery-optimization exemptions to $PELO_APP; mini player off for DRM streaming apps"
