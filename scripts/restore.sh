#!/usr/bin/env bash
# Put the bike back the way Peloton shipped it: uninstall our apps, undo the permission
# changes Pelo's scripts make to other apps, and make Peloton's launcher the home screen
# again. Never uninstalls or disables Peloton's packages.
#
#   scripts/restore.sh            remove our apps only
#   scripts/restore.sh --all      also remove the test apps (upstream Grupetto, NewPipe)
set -euo pipefail
source "$(dirname "$0")/common.sh"

TEST_APPS=(com.spop.poverlay org.schabi.newpipe)

packages=("${PELO_PACKAGES[@]}")
if [[ "${1:-}" == "--all" ]]; then
    packages+=("${TEST_APPS[@]}")
fi

require_device

for pkg in "${packages[@]}"; do
    case "$pkg" in
        com.peloton*|com.onepeloton*)
            echo "Refusing to touch Peloton package $pkg" >&2
            exit 1
            ;;
    esac
    if is_installed "$pkg"; then
        echo "Uninstalling $pkg"
        adb shell am force-stop "$pkg"
        adb uninstall "$pkg" >/dev/null
    else
        echo "$pkg not installed"
    fi
done

# Undo grant.sh's changes to other apps.
# Mini player back to Android's default (Netflix shipped with it blocked, so leave it).
for app in com.disney.disneyplus com.amazon.avod.thirdpartyclient com.peacocktv.peacockandroid; do
    if is_installed "$app"; then
        adb shell appops set "$app" PICTURE_IN_PICTURE default
    fi
done

echo "Setting home screen to Peloton's launcher"
adb shell cmd package set-home-activity "$PELOTON_HOME" >/dev/null
adb shell input keyevent KEYCODE_HOME

echo "Done. The bike is back on Peloton's launcher."
