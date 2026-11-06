# Nobody Remembers

Offline Android app for hikers and birders: record the path on the way out, then get walked back along the same
path (compass arrow, off-path buzz, distance and climb left, elevation profile). No internet permission, no
Google Play Services, no map tiles.

## Build

- JDK: Android Studio's JBR (`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`). No
  toolchain pin; Java/Kotlin target 17.
- `./gradlew :app:assembleDebug`, `./gradlew :app:testDebugUnitTest`.
- AGP 9.2.1 with built-in Kotlin (no `kotlin-android` plugin applied in `app/`), Kotlin 2.3.21, KSP 2.3.7 for Room.
- compileSdk 37 (Compose UI 1.12 requires it), targetSdk 36, **minSdk 33 (Android 13)**.
- Material 3 is pinned to `1.5.0-rc01` (over the BOM's 1.4.0) for the expressive components: floating toolbar,
  flexible app bars, toggle buttons, loading indicator, short navigation bar, segmented list items.

## Layout

- `geo/`: pure Kotlin, unit-tested. `Route` (projection, locate, pointAt, climbAfter), `PathFollower` (guidance;
  keeps a progress window so paths that double back don't jump), `ClimbCounter` (hysteresis ascent/descent).
- `data/`: Room (`Trail`, `TrackPoint`, `Mark`), `Gpx` writer. `Trail.endedAt == null` means still recording.
- `tracking/`: `TrackingService` (foreground, type location) does all GPS work; `Tracker` holds in-process
  StateFlows the UI reads. `AltitudeFuser` uses smoothed GPS altitude until the first good one, then anchors the barometer
  to it (no jump at the switch). `PathFollower` has on/off-path hysteresis (30 m out, 20 m back) and only
  allows "arrived" on the last 100 m, so loops don't finish at the trailhead.
- The emulator image on this Mac (API 37.1, 8 GB host) hangs under memory pressure; test on a phone over USB.
- `ui/`: theme from the Nobody Builds tokens (`ui/theme/Color.kt`), Gilroy + Unbounded (accent, one phrase per
  screen) in `res/font`, screens in `ui/screens`, canvas map / elevation chart / compass in `ui/components`.

## Design

- Palette, type and shape rules come from github.com/Nobody-Builds/Nobody-Builds-Design (tokens.json,
  docs/ui-brand-spec.md). Hard Hat yellow is the one bold action per screen; statuses are words on soft fills;
  no shadows; icons are the Rounded set.
- The app logo is `design-assets/logo-source.webp` (the builder at a fork in the trail with a question mark).
  Every logo file is generated from it by `python -I design-assets/build_logo.py` (Pillow, numpy, scipy):
  the transparent master `design-assets/logo.png`, `drawable-nodpi/logo.png`, a dark variant with the kit's
  Paper sticker border in `drawable-night-nodpi/logo.png`, and the adaptive icon's foreground and outline-only
  monochrome (themed icon) layers in `mipmap-*`. The Android 12+ splash uses the adaptive icon automatically.
  To change the logo, replace the source and rerun the script; don't edit the PNGs.
- The status-bar notification icon (`drawable/ic_stat_trail.xml`) stays a simple trail-and-question-mark
  silhouette: Android draws it as a 24 dp single-colour mask, where the full logo would be a blob.
- Empty states use the kit's sleeping builder (`res/drawable-nodpi/builder.png`, dark variant in
  `drawable-night-nodpi`), the mascot rather than the logo.
- Fonts were converted from the kit's WOFF/WOFF2 with fontTools; licences in `design-assets/font-licenses/`.
