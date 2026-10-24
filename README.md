# Seeker TV

A small, fast home screen for Google TV, drawn entirely on a canvas: a dot-matrix app wheel, plus three optional arcade "home modes" where you pick an app by playing a few seconds of a game. It replaces Google TV's own launcher, which frees a few hundred MB of RAM on low-memory TVs.

Built and tested on a Hisense 55A53FUA (Google TV, Android 11, 2 GB RAM). The full install also works on a Chromecast with Google TV (4K, `sabrina`, Android 14, 2 GB RAM), which is where v0.10.0's [background and slideshow](#background-and-slideshow-v0100) were built and tested. It should work on other Google TV / Android TV 11+ devices, but nothing else has been tried.

> **Warning.** Making Seeker your HOME screen means disabling two Google system packages (`launcherx` and `setupwraith`) over adb. It's reversible: `restore-kit/panic-home.sh` puts Google TV's home screen back, and a factory reset restores everything to stock. Do it only if you're comfortable with adb, and take the snapshot first.

## The modes

Every build has the wheel. The game modes are optional (see [Flavors](#flavors)). Switch modes in Seeker's settings: **HOME MODE**.

| | |
|---|---|
| **WHEEL**. Your apps as a dot-matrix wheel in each service's color. Up/Down spins, OK opens, hold OK to move or hide an app. Greeting, clock, date and weather on the right. | ![Wheel](docs/screens/wheel.png) |
| **FLAP**. A flappy-bird run through tubes in each service's color. Fly through a tube to open that app. A rainbow strip along the top edge doubles as bias-light input. | ![Flap](docs/screens/flap.png) |
| **INVADERS**. Your apps are the invaders. Shoot one to open it; the meteor is Settings. | ![Invaders](docs/screens/invaders.png) |
| **HUNT**. An 8-bit duck hunt: each duck is an app. Left/Right picks a duck, tap OK to shoot (1 in 4 hits), hold OK to miss on purpose. The dog pops up to react. | ![Hunt](docs/screens/hunt.png) |

Settings (gear on the wheel, SETTINGS on each game's start screen): SYSTEM SETTINGS, INPUTS, HOME MODE, SOUND, **NAME** (the greeting is HELLO &lt;NAME&gt;, default SEEKER), **WEATHER LOCATION** (any city, default Denver), HIDDEN APPS, ABOUT. The wheel's [background and slideshow](#background-and-slideshow-v0100) are set over adb.

## Flavors

One APK per flavor, all with the same app id (`com.seeker.tv`), so installing one over another just changes which modes exist and keeps your settings.

| APK | Modes |
|---|---|
| `seeker-wheel-<version>.apk` | WHEEL only (smallest; HOME MODE is hidden) |
| `seeker-flap-<version>.apk` | WHEEL + FLAP |
| `seeker-invaders-<version>.apk` | WHEEL + INVADERS |
| `seeker-hunt-<version>.apk` | WHEEL + HUNT |
| `seeker-all-<version>.apk` | all four |

Download them from [Releases](../../releases).

## Background and slideshow (v0.10.0)

*Created and tested on a Chromecast with Google TV (4K, `sabrina`, Android 14, 2 GB RAM).*

New in v0.10.0: an optional picture behind the wheel instead of the dot grid, either **one image** or a **slideshow** of a whole folder (shuffled, a new photo every N seconds, with a short cross-fade). Use your own wallpaper, or family photos copied off a NAS or Plex server. Only the wheel gets the background; Settings and the game modes keep their own look.

**CPU strain, in short:** none while idle (about 0.1%, the same as without a background); a 30-second slideshow averages about **2% CPU** with the fade or about **0.6%** with `fade false`, as a short burst at each photo change. Memory: about 80 to 110 MB while a slideshow runs, against about 26 MB for the plain wheel and about 305 MB for Google TV's own launcher. Full numbers: [How much does it cost?](#how-much-does-it-cost).

**How it looks after:** each image is rotated using its EXIF tag (phone photos aren't sideways), center-cropped to 16:9 and darkened 75% so the white dot text stays readable.

### Do it yourself

1. **Get a v0.10.0 build.** The v0.9.0 APKs don't have this. Build it (see [Build from source](#build-from-source)): `./gradlew assembleAllDebug` gives `launcher/app/build/outputs/apk/all/debug/seeker-all-0.10.0-debug.apk`.
   A debug build is signed with your own key, so it **can't install over a release APK**. Make Google TV HOME again, `adb uninstall com.seeker.tv`, install your build, then redo the HOME steps (exactly as in [INSTALL-WITH-CLAUDE.md](INSTALL-WITH-CLAUDE.md#optional-background-and-slideshow-v0100), which has a ready-made prompt). Uninstalling clears Seeker's settings: set your name, city, app order and hidden apps again. Later builds from the same computer install over it with `adb install -r`.
2. **One image:**
   ```bash
   adb push wallpaper.jpg /sdcard/Android/data/com.seeker.tv/files/background
   adb shell am broadcast -a com.seeker.tv.SET --ez background true      # false removes it
   ```
3. **Slideshow:** copy a folder of photos (JPEG, PNG or WebP) into Seeker's own folder, then turn it on:
   ```bash
   adb push my-photos/. /sdcard/Android/data/com.seeker.tv/files/backgrounds/
   adb shell am broadcast -a com.seeker.tv.SET --es slideshow 30          # seconds (min 5), or off
   adb shell am broadcast -a com.seeker.tv.SET --ez fade false            # optional: cut instead of fade
   ```
   Added or removed photos? Push them, then `SET --ez background true` re-reads the folder. With the slideshow `off`, the single image (if any) comes back.

No storage permission is needed: `Android/data/com.seeker.tv/` is the app's own folder, and adb's shell can write to it. Uninstalling Seeker deletes it.

### How much does it cost?

Measured on the Chromecast with Google TV (4K, `sabrina`, Android 14, 2 GB RAM), debug build, 79 phone photos (46 MB):

| | CPU |
|---|---|
| Idle on the wheel, any background | about 0.1% (the wheel only redraws on input) |
| Per slide, first loop (full-size photo decoded and cached) | about 1 s |
| Per slide, cached, with the 0.5 s fade | about 0.6 s, so about 2% average at 30 s |
| Per slide, cached, `fade false` | about 0.18 s, so about 0.6% average at 30 s |

The slideshow stops while another app (or the screensaver) is in front. Cached slides take about 220 KB each in the app's cache (17 MB for 79 photos); Android may clear it under storage pressure and Seeker rebuilds it. Memory: about 80 MB PSS with a 30 s slideshow and up to about 110 MB with fast 5 s slides (two full-screen bitmaps during a fade), against about 26 MB for the plain wheel and about 305 MB for Google TV's launcher.

## How it works

- **adb over Wi-Fi.** Everything is done from a computer with `adb`: install the APK, grant two optional permissions, make Seeker HOME. Nothing is rooted.
- **Why launcherx and setupwraith get disabled.** Google TV's launcher (`com.google.android.apps.tv.launcherx`) takes HOME back whenever it's enabled. Its guard, `com.google.android.tungsten.setupwraith`, has a `RecoveryActivity` that re-enables launcherx; with launcherx disabled and the guard on, it takes HOME itself and loops (a new activity every ~80 ms). Android's shell can't disable a single component on these TVs, so the whole setupwraith package is disabled, **first**, then launcherx. Side effect: Google TV setup flows (add account, re-run setup) may not work until you re-enable them; `panic-home.sh` does that.
- **Why Hisense's `customerlauncher` is not a fallback.** It sounds like a launcher, but on this build it has no HOME intent filter (only a helper activity). If Seeker can't start, Android falls back to `com.android.tv.settings/.system.FallbackHome`, not to customerlauncher. Seeker has its own safety net: after two crashes in a row it shows a plain app list with "Retry Seeker TV" and "Settings".
- **The HOME switch.** Seeker ships its HOME entry (`.Home`) disabled, because an enabled HOME candidate can make Android drop your current preferred HOME. Seeker turns it on itself when it receives `com.seeker.tv.HOME_ALIAS`, then `cmd package set-home-activity` makes it HOME.
- **RAM.** On the reference TV, launcherx plus its `coreservices` process used about 305 MB RSS. Seeker uses about 26 MB PSS on the wheel and 35 to 47 MB PSS while a game is being played. Free RAM two minutes after boot went from 91 MB (stock) to 163 MB with Seeker as HOME. (Android quickly fills freed RAM with cached apps, so treat these as rough.)
- **Weather** comes from [Open-Meteo](https://open-meteo.com) (no API key). The city you type is looked up with Open-Meteo's geocoder; only the coordinates are stored, on the TV.
- **Optional permissions**, both granted over adb: `WRITE_SECURE_SETTINGS` lets Seeker keep animations at 0.5x (snappier), and the `SYSTEM_ALERT_WINDOW` app-op lets `com.seeker.tv.NOTIFY` broadcasts show as toasts over other apps.

## Requirements

- A Google TV / Android TV device on Android 11 or newer, on the same network as your computer.
- A computer with `adb` ([Android platform-tools](https://developer.android.com/tools/releases/platform-tools); on a Mac, `brew install android-platform-tools`).
- Developer options and USB debugging (network debugging) on the TV.
- To build from source: JDK 17 and the Android SDK (compileSdk 34).

## Install

The easiest way is to hand this repo to [Claude Code](https://claude.com/claude-code) and follow **[INSTALL-WITH-CLAUDE.md](INSTALL-WITH-CLAUDE.md)**. It walks through the same steps as below, one change at a time, recording an undo for each.

By hand:

1. On the TV: Settings > System > About, press OK on **Android TV OS build** 7 times. Then Settings > System > Developer options > turn on **USB debugging** (and **Network debugging** if listed).
2. `cp .env.example .env` and set `TV_IP` to your TV's IP address.
3. `adb connect <TV_IP>:5555`, then accept the prompt on the TV ("Always allow").
4. Snapshot first: `restore-kit/snapshot.sh` (read-only).
5. Install a flavor: `adb install -r seeker-all-0.9.0.apk`
6. Optional grants: `adb shell pm grant com.seeker.tv android.permission.WRITE_SECURE_SETTINGS` and `adb shell appops set com.seeker.tv SYSTEM_ALERT_WINDOW allow`
7. Open Seeker once: `adb shell am start -n com.seeker.tv/.MainActivity`
8. Make it HOME, in this order, waiting a few seconds between steps:
   ```
   adb shell am broadcast -a com.seeker.tv.HOME_ALIAS --ez enabled true
   adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
   adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
   adb shell cmd package set-home-activity --user 0 com.seeker.tv/.Home
   ```
   Wait 15 seconds (Android saves these changes on a delay), then press Home on the remote.
9. Check: `adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME` ends with `com.seeker.tv/.Home`. Reboot (`adb reboot`) and confirm Seeker comes back.
10. Set your name and city in Seeker's settings, or over adb: `adb shell am broadcast -a com.seeker.tv.SET --es name ALEX`

Updating later: `adb install -r` the new APK. HOME stays on Seeker.

**Already installed a debug build you compiled yourself?** Release APKs are signed with a different key, so `adb install -r` will refuse (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). Uninstall the debug build first (`adb uninstall com.seeker.tv`; run `panic-home.sh` first if Seeker is HOME), then install the release and redo step 8.

## Restore kit

`restore-kit/` reads `TV_IP` and `ADB` from `.env`.

| Script | What it does |
|---|---|
| `snapshot.sh` | Read-only. Saves your TV's packages, HOME, settings and free RAM to `restore-kit/snapshot-<date>/`. |
| `post-update-check.sh` | Read-only. Compares the TV with `expected-state.txt` and prints PASS / DRIFT with a fix command for each DRIFT. Run it after a firmware update: OTAs can re-enable launcherx. |
| `panic-home.sh` | Home screen black, looping or crashing: re-enables launcherx and setupwraith, makes Google TV HOME, waits 15 s, reboots. |
| `restore-stock.sh` | Undoes everything in `expected-state.txt` (the reference TV's debloat plus the Seeker install) and reboots. |

`RESET.md` covers factory resets, including when the screen is unusable. **A factory reset always brings the TV back to stock.**

`expected-state.txt` describes the reference TV, which also had an optional debloat (17 disabled packages, AdGuard private DNS, a plain screensaver). Seeker needs none of that; on your TV those lines just show as DRIFT. Delete the lines you don't want checked.

## Build from source

```
cd launcher
./gradlew assembleAllDebug                 # or assembleWheelDebug, assembleHuntDebug, ...
./gradlew assembleRelease                  # all five, unsigned unless the signing env vars are set
```

Release signing reads `SEEKER_KEYSTORE`, `SEEKER_KEYSTORE_PASSWORD`, `SEEKER_KEY_ALIAS` and `SEEKER_KEY_PASSWORD` from the environment. APKs land in `launcher/app/build/outputs/apk/<flavor>/release/seeker-<flavor>-<version>.apk`.

Source layout: shared code in `app/src/main`, each game in `app/src/modeFlap`, `modeInvaders`, `modeHunt`, and each flavor's mode list in `app/src/<flavor>/.../BuiltModes.kt`. The approved design mockups (open them in a browser) and specs are in `design/`; older mockups in `design/archive/`.

## Trademarks and art

Streaming service names (Netflix, YouTube, Hulu, Prime Video, Disney+, Max, Peacock, Apple TV) and their colors are used only to identify apps installed on your TV. They are trademarks of their owners; this project isn't affiliated with or endorsed by any of them. **No logos are bundled**: the app icons on Flap's tubes are loaded at runtime from the apps already installed on your TV. All game art (the bird, invaders, ducks, dog, tree, shotgun) and all sounds are original, drawn and synthesized for this project in the style of the era, not copied from any game.

## License

MIT, see [LICENSE](LICENSE).
