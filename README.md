<p align="center"><img src="design-assets/logo.png" width="240" alt="The builder at a fork in the trail, scratching his head under a blue question mark"></p>

# Nobody Remembers

Walk out, forget the way, get walked back. An offline Android app for hiking and bird watching.

- **Record a trail** before you set off. GPS keeps recording with the screen off; altitude comes from the
  barometer (anchored to GPS) when the phone has one.
- **Mark spots** on the way: birds, junctions, water, views, hazards, notes.
- **Take me back** follows *your* recorded path in reverse: a compass arrow, "bear left / turn round" hints,
  distance, climb and descent left, a walking-time estimate, the next mark coming up, a to-scale trail view
  and an elevation profile with you on it. It buzzes if you stray more than 30 m and when you arrive.
- **Trails** keeps every walk with its stats. Follow any of them back or forward again, rename, delete, or
  export GPX.

Nothing needs the internet. There's no internet permission at all.

Android 13 or later. Built with Kotlin, Jetpack Compose and Material 3 Expressive, in the
[Nobody Builds](https://github.com/Nobody-Builds/Nobody-Builds-Design) palette.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```
