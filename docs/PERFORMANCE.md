# Performance: Baseline Profiles, macrobenchmarks, streaming metrics

Plan items **E16-01** (Baseline Profiles), **E16-02** (macrobenchmarks) and **E16-03** (streaming
metrics), measured against the targets in PRODUCT_SPEC §9:

| Target (spec §9) | How it's measured |
|---|---|
| Cold start → Home rendered ≤ 1.5 s from cache (mid-range phone) | `StartupBenchmark`: `timeToFullDisplayMs` (the app reports fully drawn when Home has rails + hero) and `HomeFirstRailFirstMs` |
| Rails hold 60 fps on phone | `HomeScrollBenchmark`: `frameDurationCpuMs` p50/p90 ≤ 16.7 ms, `frameOverrunMs` p90 ≤ 0 |
| Details → first source row ≤ 3 s (p50) | `DetailsBenchmark`: `SourcesFirstRowFirstMs` (tap → first SourceRow composed), plus a UiAutomator cross-check |
| TTFF ≤ 15 s p50 / ≤ 30 s p90 | `JazzyMetrics` log line on first frame (player), `PlayerFirstFrame` trace section, `core/torrent` `TorrentSpikeTest` (`JazzySpike`) |

## Results

First run: **2026-10-11**, `benchmark` 1.5.0, `benchmarkRelease` build (R8, Baseline Profile from
this commit). Device: **`Medium_Phone_API_37.0` emulator** (arm64, API 37, 4 GB, **software GPU**
`-gpu swiftshader_indirect`, headless), on a host that was heavily loaded by other builds. The
physical Pixel was locked, so it wasn't used. **Treat these as a smoke test and a None-vs-profile
comparison, not as §9 numbers.** Startup is roughly indicative; frame timings are not (every frame is
rendered on the CPU). Re-run on a mid-range phone and a Chromecast with Google TV before judging §9.

Medians of 10 (startup) or 5 (others) iterations; frame metrics are P50 / P90 over all frames.

| Benchmark | Metric | None (no AOT) | Baseline Profile | §9 target |
|---|---|---|---|---|
| Cold start | `timeToInitialDisplayMs` | 643 | 589 | – |
| Cold start | `timeToFullDisplayMs` (process start → Home with rails) | 1539 | **1247** (−19%) | ≤ 1500 |
| Cold start | `HomeFirstRailFirstMs` (Activity.onCreate → Home ready) | 991 | 677 | – |
| Warm start | `timeToInitialDisplayMs` | 398 | 240 | – |
| Warm start | `timeToFullDisplayMs` | 1044 | 993 | – |
| Home scroll | `frameDurationCpuMs` P50 / P90 | 67.8 / 81.8 | 68.0 / 80.0 | ≤ 16.7 (60 fps) — not measurable on swiftshader |
| Home scroll | `frameOverrunMs` P50 / P90 | 74.2 / 96.6 | 74.0 / 93.1 | ≤ 0 |
| Details | `SourcesFirstRowFirstMs` (tap → first source row) | 371 | **193** | ≤ 3000 (p50) |
| Details | `frameDurationCpuMs` P50 / P90 | 47.7 / 73.3 | 47.6 / 61.9 | – |
| Details (re-run) | `SourcesFirstRowFirstMs` | 207 | 213 | ≤ 3000 (p50) |
| Details (re-run) | UiAutomator tap → row on screen, p50 / p90 | 2734 / 2757 | 2837 / 3065 | – |

Reading it:
- The Baseline Profile cuts cold start to Home with content by ~290 ms (−19%) and warm-start
  first frame by ~40%. Cold start with the profile is inside the 1.5 s target
  even on a software-rendered emulator. Confirm on a real mid-range phone.
- The two Details measures differ by design. `SourcesFirstRow` ends when the Details state first has
  rows (the frame where the list could show them). The UiAutomator number adds tap injection,
  accessibility idle waits and a scroll to bring the list (below the fold on phones) on screen, all
  slow on a CPU-rendered emulator, so it's an upper bound. The None/profile difference in the first
  run (371 vs 193 ms) didn't repeat in the re-run (207 vs 213 ms); with 5 iterations on an emulator
  that's noise.
- `SourcesFirstRow` came in at 0.15–0.5 s because the benchmark taps the same title every iteration,
  so sources come from the source cache after the first search (§9 allows "from a cached result").
  An uncached search is network-bound; measure it with a fresh title on a real network.
- Home and Details frame timings sit at ~50–70 ms per frame because swiftshader renders every frame
  on the CPU. They say nothing about the 60 fps target.
- TTFF (E16-03) has no numbers yet: there's no streaming in benchmarks by design. Collect from
  `JazzyMetrics` on real plays (below).

Raw results land in `baselineprofile/build/outputs/connected_android_test_additional_output/benchmarkRelease/connected/<device>/`
(each run overwrites them).

## Modules and files

- `:baselineprofile` (`com.android.test` + `androidx.baselineprofile`, target `:app`)
  - `BaselineProfileGenerator`: `startup` (cold start → Home; also feeds the startup/dex-layout
    profile) and `journeys` (Home rails vertical + horizontal, a movie's Details and its Sources list,
    Search typing). Never presses Play.
  - `StartupBenchmark`: cold/warm × `CompilationMode.None` / `Partial(BaselineProfile)`.
  - `HomeScrollBenchmark`: `FrameTimingMetric` over Home vertical flings + rail scrolls.
  - `DetailsBenchmark`: `TraceSectionMetric("SourcesFirstRow")` + `FrameTimingMetric`.
  - `JazzyJourneys.kt`: shared UiAutomator steps. Tolerant of where the app starts: accepts the
    disclaimer, creates a profile named **Bench** on a first run (genre step skipped), or picks a
    profile on the picker. Before startup measurements it turns on "Remember my profile" (only when
    the picker shows at launch, i.e. the setting is off) so a cold start lands on Home.
- `:app`
  - `androidx.baselineprofile` plugin applied; `baselineProfile(projects.baselineprofile)`;
    `dexLayoutOptimization = true`; `automaticGenerationDuringBuild = false` (profiles are generated
    on a device and committed).
  - Generated profiles: `app/src/release/generated/baselineProfiles/{baseline,startup}-prof.txt`.
    They ship in `release` (and the plugin's `benchmarkRelease`). `profileinstaller` installs them on
    sideloaded installs.
  - `nonMinifiedRelease` / `nonMinifiedInternal` are declared in `app/build.gradle.kts` with
    `optimization { enable = false }`. The plugin would create them itself but only clears the
    legacy `isMinifyEnabled`, which doesn't override AGP 9's `optimization.enable` copied from
    `release` — the capture build would then be R8-processed and the profile would name obfuscated
    classes. `benchmarkRelease` is created by the plugin from `release` (R8 on, debug-signed,
    profileable).
  - Trace points (`app/src/main/kotlin/com/nexient/jazzy/perf/PerfMarkers.kt`), all async sections:
    - `HomeFirstRail`: `MainActivity.onCreate` → Home state has rails and a hero (or is offline).
      Same moment the app calls `reportFullyDrawn` (`ReportDrawnWhen`), so `timeToFullDisplayMs`
      is "launch → Home with content".
    - `SourcesFirstRow`: `Navigator.push(DetailsRoute)` (the tap) → first source row composed. The
      end is one line in `feature/details/.../DetailsScreen.kt`.
    - `PlayerFirstFrame`: `Navigator.push(PlayerRoute)` → ExoPlayer `onRenderedFirstFrame`
      (listener on the shared `PlayerHolder` player).
- `feature/player/.../PlayerViewModel.kt`: on the first `STATE_READY`, logs
  `JazzyMetrics: first_frame ttffMs=… metadataMs=… file=… stats=…` (E16-03).

Catalog entries added: `benchmark = 1.5.0` (`androidx.benchmark:benchmark-macro-junit4`, plugin
`androidx.baselineprofile`), `uiautomator = 2.4.0`, `tracing = 1.3.0` (`androidx.tracing:tracing`),
plugin alias `android-test` (`com.android.test`). The test module reuses the existing
`androidx-junit` (1.3.0) and `androidx-test-runner` (1.7.0) aliases. Tracing stays on the 1.x line
because Compose resolves `androidx.tracing` 1.2 today; 2.0 is a KMP rewrite we can move to with Compose.

## How to run

Use a **physical, unlocked** mid-range phone for numbers you compare against §9. Emulators are fine
for checking the journeys work. Pin the device with `ANDROID_SERIAL` — the connected-test tasks
otherwise install on **every** attached device (including TVs on adb-over-Wi-Fi).

```sh
export ANDROID_SERIAL=<serial>              # adb devices

# E16-01: regenerate the Baseline Profile (API 33+ or rooted; ~15 min on an emulator)
./gradlew :app:generateReleaseBaselineProfile
#   -> app/src/release/generated/baselineProfiles/{baseline,startup}-prof.txt  (commit both)

# E16-02: all macrobenchmarks (benchmarkRelease build: R8, debug-signed, profileable)
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
#   one class only:
./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.nexient.jazzy.baselineprofile.StartupBenchmark
#   on an emulator add:
#   -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

Results: `baselineprofile/build/outputs/connected_android_test_additional_output/benchmarkRelease/connected/<device>/`
(`*-benchmarkData.json` plus one Perfetto trace per iteration — open in https://ui.perfetto.dev and
search for `HomeFirstRail` / `SourcesFirstRow` / `PlayerFirstFrame`). Android Studio shows the same
summary in the test output. The Details UiAutomator cross-check is in logcat:
`adb logcat -s JazzyBench`.

Notes:
- The journeys need network on the first run (TMDB for Home, source sites for Details). Startup is
  measured after the first setup has warmed the TMDB cache, i.e. "from cache" as §9 specifies.
- `DetailsBenchmark` includes live source-site latency, so it varies with the network.
- Benchmarks leave the app with a profile named **Bench** and "Remember my profile" on. Run them on a
  test device, not one with real profiles you care about.

### Streaming metrics (E16-03)

```sh
adb logcat -s JazzyMetrics     # one line per playback start: ttffMs, metadataMs, file, stats
adb logcat -s JazzySpike       # core/torrent TorrentSpikeTest (connectedDebugAndroidTest) TTFF/seek numbers
```

Record TTFF p50/p90 from ≥ 10 plays of healthy (≥ 50 seeders) public-domain sources per reference
device. A `PlayerFirstFrame` slice is also in any Perfetto trace recorded while pressing Play.

## Known issues found while profiling

1. **Release crashed at startup (fixed in `app/src/main/keepRules/rules.keep`).** With
   `optimization { packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**") }`, R8 repackaged
   `kotlin.text.StringsKt__StringBuilderKt` into the root package while its package-private superclass
   part stayed in `kotlin.text`, so the first string helper call (Room database creation) threw
   `IllegalAccessError: Class kotlin.text.n extended by class ve3 is inaccessible`. The `release`
   mapping from before the Baseline Profile work shows the same move (`-> ga3`), so it wasn't caused by
   the profile. Fix: `-keep,allowshrinking,allowoptimization class kotlin.**Kt__**` (keeps the names and
   packages of Kotlin multi-file facade parts; they're still shrunk and optimized). Worth reporting
   upstream to R8 together with a minimal repro.
2. **Home crashed on a duplicate rail key (fixed in `RailPagingSource`).** `HomeRails.kt` keys paged
   rails by `"${tmdbId}:${mediaType}"`. TMDB's paged lists (Trending in particular) can return the
   same title on two pages, so flinging far enough along a rail threw
   `IllegalArgumentException: Key "97546:tv" was already used`. `RailPagingSource` now drops titles
   already loaded on an earlier page (`RailPagingSourceTest`). The journeys still use bounded rail
   scrolls so iterations stay comparable.
3. Home's "pull to refresh" fires when a benchmark flings up past the top; harmless, but it adds a
   network refresh to the scroll benchmark. Flings could stop at the top in a later pass.
