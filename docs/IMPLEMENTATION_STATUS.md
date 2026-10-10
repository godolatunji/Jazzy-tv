# Jazzy — Implementation Status

| | |
|---|---|
| **Updated** | 2026-10-11 |
| **Against** | `DEVELOPMENT_PLAN.md` v1.1 / `PRODUCT_SPEC.md` v0.4 |

## Summary
All 18 modules are in place and the app builds for phone and TV from one APK. Unit tests pass in every module (`./gradlew testDebugUnitTest`). On an emulator and a Pixel 10 Pro XL: first run (disclaimer, profile, genre picks), Home with live TMDB rails, Details with live source search across the six default sites, and deep links all work. In-app playback has been verified by the engine's instrumented spike (Sintel, Big Buck Bunny); the full in-app Play → player flow still needs a device check (see "Not yet verified").

## Done, by epic
| Epic | State | Notes |
|---|---|---|
| E0 Setup | Done | Convention plugins, 18 modules, version catalog, Java 17, ABI splits + versionCode scheme, CI workflow, debug-only `jazzy_force_tv` switch. E0-07 (managed devices) not done. |
| E1 Design system | Done | Theme tokens, focus-aware clickable, cards, rails, hero, source/episode rows, PIN pad, avatars, settings rows, chips. E1-07 screenshot tests not done. |
| E2 Database | Done | `jazzy.db` + `jazzy_cache.db`, DAOs, exported schemas, Keystore secrets, PBKDF2 PINs, backup rules, seed data. Robolectric DAO tests. |
| E3 TMDB + cache | Done | Cache-first with TTLs, paging, 8-request limit, 429 backoff, maturity filtering, eviction worker. |
| E4 Profiles | Done | Picker, editor, genre picks, profile + parental PINs, kids mode, remember profile. |
| E5 Home | Done | Hero, Continue Watching, recommendations, My List, Liked, TMDB rails incl. Originals, Top 10, genres. Phone + TV (dynamic backdrop). |
| E6 Details | Done | Sources list with Play on every row, filters/sort, resume, last-played/failed/may-not-play badges, episodes side panel, More Like This. Phone, wide and TV layouts. |
| E7 Search | Done | Debounced multi-search, recents, voice, person pages, Google TV global search provider. |
| E8 Source search | Done | TorrentSearch port (14 providers, 6 on by default), query building, release parsing, matching, cross-site de-dupe, lazy magnets, Sites settings, opt-in live smoke test. E8-12 Torznab provider not done (P1). |
| E9 Engine | Done (G1 conditional go) | libtorrent4j engine, graded piece scheduler, data source, loopback server, cache. See plan §17 for the G1 numbers. |
| E10 Player | Mostly done | Controls, gestures, PiP, media session, sources/episodes/tracks panels, next episode, stall prompt, errors, external player. E10-11 FFmpeg deferred (licence). E10-13 OpenSubtitles not done (P1). |
| E11 Library | Done | My List, Liked (undo keeps dates), resume points, sessions, History grouping/folding/deletion. |
| E12 Recommendations | Done | Unified genres, affinity scoring, rail builders, triggers + worker. |
| E13 Settings | Done | All §5.11 sections, local data sizes, clear caches. |
| E14 TV | Mostly done | Drawer, focus, TV layouts for every screen, Watch Next. Not checked on a real TV or TV emulator yet (no TV system image installed). |
| E15 Discover | Done | Filters, sort, infinite grid; kids profiles keep their filters. |
| E16 Performance | Not started | Baseline Profiles, macrobenchmarks. |
| E17 A11y / l10n | Partial | Content descriptions on key controls; strings are not externalised yet. |
| E18 Privacy / security | Partial | Disclaimer, opt-in toggles. Crash reporting backend waits on D9. |
| E19 Release | Partial | Signing config, release pipeline and in-app updater wait on D1. |

## Not yet verified
- In-app Play → player on a device (the phone locked during the test).
- TV layouts on a TV device or TV emulator.
- The 20-run streaming protocol on reference devices (plan §10.3).

## Known gaps and follow-ups
- Scheduler tuning for seek latency on higher-bitrate files (G1).
- TE-10 ratio cap and "0 upload on cellular" aren't enforced yet.
- UI strings are hard-coded in Kotlin; they need moving to resources.
- Decisions still open: D1 distribution, D2 legal review, D9 crash reporting, D10 TMDB terms.
