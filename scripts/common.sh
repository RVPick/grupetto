# Shared settings for the helper scripts. Source this, don't run it.

# Our own packages. restore.sh uninstalls exactly these and nothing else.
PELO_PACKAGES=(dev.pickture.pelo)
PELO_APP=dev.pickture.pelo
PELO_ACTIVITY=dev.pickture.pelo/com.spop.poverlay.MainActivity

# Peloton's stock launcher: the way back if anything goes wrong.
PELOTON_HOME=com.peloton.launcher/.LauncherActivity

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

require_device() {
    command -v adb >/dev/null || { echo "adb not found (sudo pacman -S android-tools)" >&2; exit 1; }
    local state
    state="$(adb get-state 2>/dev/null || true)"
    if [[ "$state" != "device" ]]; then
        echo "No authorized device. Check the USB cable and run 'adb devices'." >&2
        exit 1
    fi
}

is_installed() {
    adb shell pm list packages "$1" | grep -qx "package:$1"
}
