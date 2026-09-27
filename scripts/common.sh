# Shared settings for the helper scripts. Source this, don't run it.

# Our own packages. restore.sh uninstalls exactly these and nothing else.
PELO_PACKAGES=(dev.pickture.pelo)
PELO_APP=dev.pickture.pelo
PELO_ACTIVITY=dev.pickture.pelo/com.spop.poverlay.MainActivity
PELO_HOME=dev.pickture.pelo/com.spop.poverlay.launcher.HomeActivity

# Peloton's stock launcher: the way back if anything goes wrong.
PELOTON_HOME=com.peloton.launcher/.LauncherActivity

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Last known Wi-Fi address of the bike, written by scripts/wifi.sh.
BIKE_IP_FILE="$PROJECT_DIR/.bike-ip"
BIKE_ADB_PORT=5555

# Pick which connected device adb talks to, preferring Wi-Fi (so pedaling
# can't knock the connection loose). Reconnects to the last Wi-Fi address if needed.
select_device() {
    [[ -n "${ANDROID_SERIAL:-}" ]] && return
    local devices wifi
    devices="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
    wifi="$(grep ':' <<<"$devices" | head -1 || true)"
    if [[ -z "$wifi" && -f "$BIKE_IP_FILE" ]]; then
        adb connect "$(cat "$BIKE_IP_FILE"):$BIKE_ADB_PORT" >/dev/null 2>&1 || true
        devices="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
        wifi="$(grep ':' <<<"$devices" | head -1 || true)"
    fi
    if [[ -n "$wifi" ]]; then
        export ANDROID_SERIAL="$wifi"
    elif [[ -n "$devices" ]]; then
        export ANDROID_SERIAL="$(head -1 <<<"$devices")"
    fi
}

require_device() {
    command -v adb >/dev/null || { echo "adb not found (sudo pacman -S android-tools)" >&2; exit 1; }
    select_device
    local state
    state="$(adb get-state 2>/dev/null || true)"
    if [[ "$state" != "device" ]]; then
        echo "Can't reach the bike. Check Wi-Fi, or plug in USB and run scripts/wifi.sh." >&2
        exit 1
    fi
}

is_installed() {
    adb shell pm list packages "$1" | grep -qx "package:$1"
}
