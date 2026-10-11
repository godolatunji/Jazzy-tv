<p align="center">
  <img src="docs/brand/jazzy-tv-logo.png" alt="Jazzy Tv" width="240">
</p>

<h1 align="center">Jazzy Tv</h1>

<p align="center">
  Browse films and TV shows from TMDB and stream them over BitTorrent with a built-in player,
  on Android phones, tablets and Android TV from one APK.
</p>

---

## Features

- **Netflix-style Home**: multiple profiles (with kids profiles and PINs), a hero billboard, and
  rails for Continue Watching, Trending, Popular, Top Rated, Upcoming, Originals and Discover.
- **Recommendations** from each profile's watch history, using TMDB genres, computed on the device.
- **Sources from torrent sites**: each title page lists every magnet link found across the enabled
  sites, sorted by seeders, with quality, codec and audio badges. Play starts from a source row.
- **Streaming player**: libtorrent pulls pieces from many peers on demand, in playback order, so a
  film starts before it has downloaded. Seeking jumps the download to the new position.
- **Resume and history**: stop times, play history, My List and Liked, stored per profile.
- **Episodes side panel** for shows, with per-episode progress.
- **Audio and subtitles**: DTS and Dolby TrueHD decode in software (LGPL FFmpeg) when the device
  can't, and OpenSubtitles search, download and sync.
- **Android TV**: D-pad navigation, a collapsible navigation drawer, Watch Next, global search
  and deep links.

## Requirements

- Android Studio with Android Gradle Plugin 9.4 support.
- JDK 25 for the Gradle daemon. Gradle provisions it automatically
  (`gradle/gradle-daemon-jvm.properties`).
- A device or emulator on Android 9 (API 28) or newer. The app targets API 37.

## Setup

Add your API keys to `local.properties` in the project root. The file is gitignored; never commit
keys.

```properties
# Required: TMDB API Read Access Token (v4 "Bearer" token)
# https://www.themoviedb.org/settings/api
TMDB_TOKEN=your-tmdb-read-access-token

# Optional: enables subtitle search in the player
# https://www.opensubtitles.com/consumers
OPENSUBTITLES_API_KEY=your-opensubtitles-api-key
```

Both can also be supplied as environment variables with the same names.

## Build and run

```bash
./gradlew assembleDebug                 # debug APKs (one per ABI plus universal)
./gradlew :app:installDebug             # install on the connected device
```

APKs are written to `app/build/outputs/apk/<buildType>/`. With several devices attached, set
`ANDROID_SERIAL=<serial>` so Gradle installs on one only.

To preview the TV layout on a phone or emulator (debug builds only):

```bash
adb shell settings put global jazzy_force_tv 1   # 0 to turn it off
```

### Release builds

```bash
./gradlew :app:assembleRelease
```

Release builds are minified with R8 and include the Baseline Profile. No signing config is
committed, so the output is unsigned; sign it with your own release key (for example with
`apksigner`) before installing or distributing.

## Tests

```bash
./gradlew testDebugUnitTest lintDebug

# On a device: FFmpeg DTS/TrueHD decoding and an end-to-end torrent streaming test
# (streams the CC-licensed Blender films Sintel and Big Buck Bunny)
ANDROID_SERIAL=<serial> ./gradlew :core:ffmpeg:connectedDebugAndroidTest :core:torrent:connectedDebugAndroidTest
```

Performance work (Baseline Profiles, startup, scroll and Details macrobenchmarks) lives in
`:baselineprofile`. See [docs/PERFORMANCE.md](docs/PERFORMANCE.md) for results and how to rerun:

```bash
ANDROID_SERIAL=<serial> ./gradlew :app:generateReleaseBaselineProfile
```

## Project structure

| Module | What it does |
|---|---|
| `:app` | Activity, navigation (Navigation 3), phone bottom bar and TV drawer, Watch Next, search provider |
| `:core:model` | Plain Kotlin models |
| `:core:common` | Dispatchers, clock, `Resource` wrapper and formatting helpers |
| `:core:network` | TMDB and OpenSubtitles APIs (Retrofit, kotlinx.serialization) |
| `:core:sources` | Torrent-site search engine and providers, cross-site de-duplication |
| `:core:database` | Room: `jazzy.db` (profiles, history, lists) and `jazzy_cache.db` (TMDB cache) |
| `:core:data` | Repositories, cache-first loading, settings, device capabilities |
| `:core:recommendations` | On-device genre affinity |
| `:core:torrent` | libtorrent4j streaming engine and piece scheduler, Media3 data source |
| `:core:player` | Media3 ExoPlayer setup and playback service |
| `:core:ffmpeg` | LGPL FFmpeg audio decoders (prebuilt; see its `LICENSE-NOTICE.md`) |
| `:core:designsystem` | Theme, components and TV focus treatment |
| `:feature:*` | `profiles`, `home`, `details`, `search`, `discover`, `library`, `player`, `settings` |
| `:baselineprofile` | Baseline Profile generator and macrobenchmarks |

Build logic is shared through convention plugins in `build-logic/`, and dependency versions are in
`gradle/libs.versions.toml`.

## Documentation

- [Product specification](docs/PRODUCT_SPEC.md)
- [Development plan](docs/DEVELOPMENT_PLAN.md)
- [Implementation status](docs/IMPLEMENTATION_STATUS.md)
- [Performance](docs/PERFORMANCE.md)

## Licences

Jazzy Tv bundles FFmpeg 7.1.5, built LGPL-only and linked dynamically. Every APK you distribute
must be accompanied by the corresponding FFmpeg source. Generate that bundle with
`release/package-lgpl-sources.sh` and publish its output next to the APKs; details are in
[core/ffmpeg/LICENSE-NOTICE.md](core/ffmpeg/LICENSE-NOTICE.md).

This product uses the TMDB API but is not endorsed or certified by TMDB.

## Disclaimer

Jazzy Tv doesn't host, upload or store any films or TV shows. It searches third-party torrent
sites and streams the file you choose directly from other people (peer to peer), which also shares
pieces of that file with them while you watch. You are responsible for what you search for, stream
and share, and for following the copyright law where you live. Only watch content you have the
right to watch.
