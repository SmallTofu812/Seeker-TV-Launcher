# Changelog

## v0.10.0 (2026-10-24)

Wheel background and photo slideshow.

- **Background image:** an optional picture behind the wheel instead of the dot grid. Push it to `/sdcard/Android/data/com.seeker.tv/files/background` and send `SET --ez background true` (`false` removes it). No storage permission needed.
- **Slideshow:** push a folder of photos to `files/backgrounds/` and send `SET --es slideshow 30` (seconds, min 5, or `off`). Shuffled, 500 ms cross-fade (`SET --ez fade false` for a cut), paused while another app or the screensaver is in front.
- Images are EXIF-rotated, center-cropped to 16:9 and dimmed 75% so the dot text stays readable. Slides are cached as 1920x1080 JPEGs after their first showing, so later loops are cheap.
- Measured on a Chromecast with Google TV: idle cost unchanged (about 0.1% CPU); a 30 s slideshow averages about 2% CPU with the fade, about 0.6% without.
- **Do it yourself:** see README > Background and slideshow, or the ready-made prompt in INSTALL-WITH-CLAUDE.md. Build from source (`./gradlew assembleAllDebug`); a self-built APK can't install over a release APK, so uninstall first after making Google TV HOME again.
- No v0.10.0 release APKs yet: release signing needs the project's key.

## v0.9.0 (2026-10-22)

First public release.

- **Modes:** WHEEL (dot-matrix app wheel with greeting, clock, date and weather), FLAP (fly through an app's tube to open it), INVADERS (shoot an app to open it), HUNT (8-bit duck hunt: tap OK to shoot, 1 in 4 hits; hold OK to miss).
- **Flavors:** `wheel`, `flap`, `invaders`, `hunt` and `all`. Each compiles only its game modes; HOME MODE lists only what's built in. Same app id, so flavors install over each other.
- **New settings:** NAME (greeting HELLO &lt;NAME&gt;, default SEEKER) and WEATHER LOCATION (any city via Open-Meteo's geocoder, default Denver). Both can also be set with the `com.seeker.tv.SET` broadcast.
- Clock and weather follow the TV's time zone.
- Release APKs are signed with the project's release key (`seeker-<flavor>-0.9.0.apk`).
- Restore kit reads the TV address from `.env`; new read-only `snapshot.sh`.
- Docs: README, INSTALL-WITH-CLAUDE.md (agent-guided install) and CLAUDE.md (rules for that agent).
