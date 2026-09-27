#!/usr/bin/env bash
# Switch adb to Wi-Fi so the USB cable can come out. Needs the bike on USB.
# Run again after the bike reboots (it forgets TCP mode on restart).
set -euo pipefail
source "$(dirname "$0")/common.sh"

usb="$(adb devices | awk 'NR > 1 && $2 == "device" && $1 !~ /:/ { print $1 }' | head -1)"
if [[ -z "$usb" ]]; then
    echo "Plug the bike in over USB first." >&2
    exit 1
fi

ip="$(adb -s "$usb" shell ip -f inet addr show wlan0 | grep -oE 'inet [0-9.]+' | cut -d' ' -f2)"
if [[ -z "$ip" ]]; then
    echo "The bike isn't on Wi-Fi." >&2
    exit 1
fi

adb -s "$usb" tcpip "$BIKE_ADB_PORT" >/dev/null
sleep 2
adb connect "$ip:$BIKE_ADB_PORT"
echo "$ip" > "$BIKE_IP_FILE"
echo "Connected over Wi-Fi at $ip. You can unplug the USB cable."
