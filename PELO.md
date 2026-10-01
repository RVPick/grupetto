# Pelo: personal fork of Grupetto

This `pelo` branch builds on [doudar/grupetto](https://github.com/doudar/grupetto) to make a custom launcher and ride overlay for an original Peloton Bike with no membership. `develop` (upstream's main branch) stays identical to upstream; our work lives on `pelo`.

Grupetto has no license, so this fork is for personal use on our own bike only. Don't publish builds.

Differences from upstream:

- Application ID `dev.pickture.pelo` (the Kotlin package stays `com.spop.poverlay` to keep upstream fixes easy to apply), so it installs alongside, not over, upstream Grupetto.
- The update checker points at `RVPick/grupetto` releases.
- Helper scripts in `scripts/`.
- A home screen (`launcher/HomeActivity`): Start/End ride, pinned app tiles (hold to unpin, pin more from All apps), and a Peloton button back to the stock launcher.

Design reference: https://claude.ai/artifact/FtYen1SV3nSkt6xDdwz7si (1280×720 dp screens)

### Taking upstream fixes

Pelo has replaced most of Grupetto's overlay and settings UI, so merging upstream wholesale would conflict. Take individual fixes instead, mainly sensor, Bluetooth/FTMS and heart-rate changes, which track Peloton's undocumented internals:

1. `scripts/upstream-check.sh` lists upstream commits not yet in `pelo`, grouped by area (`-v` shows the files each one touches).
2. On `pelo`, run `git cherry-pick <sha>` for each fix you want, then `scripts/deploy.sh` and test on the bike.
3. Optionally keep the fork's `develop` in sync: `git switch develop && git merge --ff-only upstream/develop && git push origin develop && git switch pelo`.

## Safety rules

- Never disable or uninstall Peloton's packages (`com.peloton.*`, `com.onepeloton.*`) or its launcher. They are the way back.
- `scripts/restore.sh` uninstalls our apps and makes Peloton's launcher the home screen again. It refuses to touch Peloton packages.
- Recovery path if something breaks: the "P" nav button (launcher chooser), then ADB (`scripts/restore.sh`), then factory reset from Settings, then recovery mode.

## Scripts

| Script | What it does |
|---|---|
| `scripts/wifi.sh` | Switch adb to Wi-Fi (needs USB once; rerun after the bike reboots). The other scripts then prefer Wi-Fi and reconnect to the last known address automatically. |
| `scripts/deploy.sh [--debug]` | Build, `adb install -r`, grant permissions, launch. Release by default; `--debug` is much slower on the bike. |
| `scripts/restore.sh [--all]` | Uninstall our app and restore Peloton's home screen (`--all` also removes upstream Grupetto and NewPipe) |
| `scripts/set-home.sh` | Make Pelo the home screen (the Home / "P" button). Undo with `restore.sh`, or tap **Peloton** on Pelo's home screen for a one-off visit. |
| `scripts/upstream-check.sh [-v]` | List upstream Grupetto commits not yet in `pelo`, grouped into sensor/Bluetooth/heart rate (worth reviewing), overlay UI (usually skip) and other |
| `scripts/grant.sh` | Grant overlay, package-install, location and battery-optimization exemptions over ADB. Changing permissions kills the running app, so relaunch afterwards. |
| `scripts/launch.sh <app>` | Open an app on the bike (`netflix`, `newpipe`, `grupetto`, `peloton`, `pelo`, or a package name) |

## Device facts (Phase 0, 2026-09-27)

| | |
|---|---|
| Model | `PLTN-RB1VO` (original Bike) |
| OS | Android 11 (SDK 30), build `RO.250111.A`, security patch 2022-10-05, `user`/`release-keys` |
| CPU | `arm64-v8a` only (no 32-bit support) |
| Screen | 1920×1080 px at 240 dpi = **1280×720 dp** (matches the mockups 1:1) |
| RAM | ~2 GB total, ~490 MB free while streaming video |
| Google Play Services | Not installed |
| DRM | Widevine plugin present and `liboemcrypto.so` exists (probably L1), not yet confirmed with an HD stream |
| Bluetooth | BLE peripheral advertising works (Grupetto advertised FTMS, Cycling Power and CSC) |

Results:

- **Sideloading** over ADB works with no membership.
- **Sensor data** works with no membership. Grupetto v0.0.40 (doudar fork) showed live power, cadence, resistance and speed. The sensor service is `com.peloton.service.SensorData/com.peloton.sensor.SensorService` (action `android.intent.action.peloton.SensorData`). `com.onepeloton.affernetservice` also exposes `IBikeInterface`.
- **Overlays** draw over video apps (tested over NewPipe). Android hides third-party overlays while Settings is in front.
- Peloton's "subscription removed" banner is drawn over every app. It can be dismissed with × but comes back.
- Netflix (v9.40) is preinstalled by Peloton's device manager (`com.onepeloton.dm.android`).

## Streaming apps (Phase 5, 2026-09-27)

The bike has no Google Play Store or Play Services. Apps come from **Aurora Store** (F-Droid build, signed in anonymously), which downloads from Google Play without a Google account. Aurora has been granted install, storage and battery exemptions over ADB.

| Service | App on the bike | Notes |
|---|---|---|
| Netflix | `com.netflix.mediaclient` | Preinstalled by Peloton. |
| Disney+ | `com.disney.disneyplus` | From Aurora. |
| Prime Video | `com.amazon.avod.thirdpartyclient` | From Aurora. Aurora tags it "Requires GSF", but it opens to Amazon sign-in. |
| Peacock | `com.peacocktv.peacockandroid` | From Aurora. |
| YouTube | SmartTube, `org.smarttube.stable` | The official app needs the Play Store and closes immediately, so it was removed. SmartTube is sideloaded from GitHub (MIT). It hides the nav bar; swipe up from the bottom edge for the P button. |
| Twitch | Xtra, `com.github.andreyasadchy.xtra` | Google Play reports the official app as "not supported" on this device. Xtra is sideloaded from GitHub (AGPL-3.0). |

NewPipe (`org.schabi.newpipe`) is also still installed.

Signing in and HD playback haven't been tested yet. HD depends on Widevine L1, which is likely but unconfirmed.

## Boot behavior (tested 2026-09-27)

After `scripts/set-home.sh`, Pelo holds Android's HOME role (`dumpsys role`), which persists across reboots. On a cold boot Pelo's home screen came up about 24 s after the reboot, right after Android's `FallbackHome`. Peloton's boot receivers ran but never showed its launcher or activation screen; the only Peloton activity was `com.onepeloton.dm.android/.DummyActivity`, which is invisible. The tablet has no device owner or active device admin, so Peloton can't force its launcher through device policy.

A reboot turns off ADB over Wi-Fi. Plug in USB and run `scripts/wifi.sh` to turn it back on.

## Peloton's activation app can't be closed from Pelo (tested 2026-09-30)

`com.peloton.activity` (the "Activate your Bike" and classes app, 100–180 MB) is a privileged system app signed with the platform key and holding `INTERNAL_SYSTEM_WINDOW`. It shows the "subscription removed" banner and a full-screen transparent `SECURE_SYSTEM_OVERLAY` window. Android treats an app with on-screen windows as visible, so `killBackgroundProcesses` (what Pelo's Close uses) can't stop it, and turning off its `SYSTEM_ALERT_WINDOW` app op has no effect because system windows bypass that switch. Pelo's "Peloton" entry in Running apps closes the launcher and workout service when it can. Freeing the activation app needs `adb shell am force-stop com.peloton.activity` (until it's next opened) or disabling the package.
