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
# Keep the overlay service alive during long rides.
adb shell dumpsys deviceidle whitelist "+$PELO_APP" >/dev/null

echo "Granted overlay, install, location and battery-optimization exemptions to $PELO_APP"
