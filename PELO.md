# Pelo: personal fork of Grupetto

This `pelo` branch builds on [doudar/grupetto](https://github.com/doudar/grupetto) to make a custom launcher and ride overlay for an original Peloton Bike with no membership. `main` tracks upstream unchanged; our work lives on `pelo`.

Grupetto has no license, so this fork is for personal use on our own bike only. Don't publish builds.

Differences from upstream:

- Application ID `dev.pickture.pelo` (the Kotlin package stays `com.spop.poverlay` to keep upstream merges easy), so it installs alongside, not over, upstream Grupetto.
- The update checker points at `RVPick/grupetto` releases.
- Helper scripts in `scripts/`.
- A home screen (`launcher/HomeActivity`): Start/End ride, pinned app tiles (hold to unpin, pin more from All apps), and a Peloton button back to the stock launcher.

Design reference: https://claude.ai/artifact/FtYen1SV3nSkt6xDdwz7si (1280×720 dp screens)

Pull upstream changes with `git fetch upstream && git switch main && git merge upstream/main && git switch pelo && git merge main`.

## Safety rules

- Never disable or uninstall Peloton's packages (`com.peloton.*`, `com.onepeloton.*`) or its launcher. They are the way back.
- `scripts/restore.sh` uninstalls our apps and makes Peloton's launcher the home screen again. It refuses to touch Peloton packages.
- Recovery path if something breaks: the "P" nav button (launcher chooser), then ADB (`scripts/restore.sh`), then factory reset from Settings, then recovery mode.

## Scripts

| Script | What it does |
|---|---|
| `scripts/deploy.sh [--release]` | Build, `adb install -r`, grant permissions, launch |
| `scripts/restore.sh [--all]` | Uninstall our app and restore Peloton's home screen (`--all` also removes upstream Grupetto and NewPipe) |
| `scripts/set-home.sh` | Make Pelo the home screen (the Home / "P" button). Undo with `restore.sh`, or tap **Peloton** on Pelo's home screen for a one-off visit. |
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
