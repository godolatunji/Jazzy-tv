# Jazzy — Product Specification

| | |
|---|---|
| **Product** | Jazzy: a movie and TV show discovery and streaming app for Android phones and Android TV |
| **Status** | Draft v0.4 |
| **Date** | 2026-10-10 |
| **Platforms** | Android phone/tablet (minSdk 34) and Android TV / Google TV (minSdk 34), from a single codebase |
| **Metadata source** | TMDB API v3 — https://developer.themoviedb.org/reference/getting-started |
| **Playback source** | Torrent sites searched by title through built-in site providers (the engine from the `TorrentSearch` project). The user picks a magnet link, and the built-in torrent engine and player stream it on demand. |
| **Local storage** | On-device SQLite (via Room) for app data, play history, resume points, My List, likes and the TMDB response cache |

### Revision history
| Version | Changes |
|---|---|
| 0.1 | Initial draft |
| 0.2 | **Sources on Details:** opening a title lists every magnet link with seeds, sorted by seeders. **Play** sits on each magnet row, and auto-pick is removed. **Local SQLite** stores play history, stop times and resume points, and caches recent TMDB fetches and general app data. **Like** button and **Liked** list (they replace My List). **History** screen. **Recommendations** from watch history, scored by TMDB genres. **Episodes side panel** for TV shows on the Details page and in the player. |
| 0.3 | **My List restored** next to Liked. My List is the "save to watch later" list. Liked records titles the user enjoyed and feeds recommendations. |
| 0.4 | **Built-in torrent-site providers** replace user-run Torznab indexers. Jazzy searches torrent sites directly, using the provider engine from the `TorrentSearch` project. Public sites are enabled by default, so sources appear with **no setup**. Adds per-site settings (enable, mirror URL, login/cookies), lazy magnet resolution, cross-site de-duplication and magnet verification. Torznab becomes an optional P1 provider. |

---

## 1. Summary

Jazzy lets a household browse movies and TV shows on a home screen styled like Netflix. Each person in the household has their own profile. Every title's metadata, artwork and catalogue rails come from TMDB.

When a user opens a title, Jazzy shows its details and searches torrent sites in the background through its built-in site providers (ThePirateBay, YTS, LimeTorrents and others, §5.8). **Every magnet link with seeders** is listed under the title, most-seeded first, and **each row has its own Play button**. For TV shows, the episode list sits **beside** the show details, as on Netflix, and choosing an episode lists that episode's magnet links. Pressing Play starts streaming right away: the built-in engine pulls pieces from many peers at once, and the player starts before the download finishes.

Jazzy keeps a **local SQLite database** of everything a profile has played: what was played, from which source, when it stopped, and the full play history. Users can resume any title from where they left off. Each profile has a **My List** of titles saved to watch later. A separate **Like** button builds a **Liked** list of titles the user enjoyed. Jazzy **suggests movies and shows** from each profile's watch history and likes, scored against TMDB's genre lists. The same database caches recent TMDB responses, so the app opens quickly and still works offline.

The same APK installs on phones and TVs. It adapts its UI to the device: touch and Material 3 on phones, D-pad focus and Compose for TV on televisions.

## 2. Goals and non-goals

### Goals
1. A Netflix-style home: profile picker, a hero billboard, and rows of titles that scroll sideways (*rails*).
2. Several local profiles per device, each with its own My List, Liked list, history, resume points, recommendations, language and maturity settings.
3. Fast discovery: search, browse by genre and Discover filters, all backed by TMDB.
4. **Open source choice:** opening a title shows every available magnet link with its seed count, sorted by seeders, and the user presses Play on the one they want.
5. **Never lose your place:** every playback is recorded locally with its stop time, so any title or episode can be resumed and the full play history browsed.
6. **Personal suggestions** from each profile's watch history and likes, based on TMDB genres, worked out entirely on the device.
7. **Works offline:** recent TMDB data is cached in local SQLite, so Home, Details, My List, Liked and History open without a network.
8. A good first-class experience on both phone (touch, portrait and landscape) and TV (10-foot UI, D-pad, remote).
9. Playback starts in **≤ 15 s** on a healthy source (see §9).

### Non-goals (v1)
- Cloud accounts or syncing profiles and history across devices (v2 candidate). All data stays on the device.
- Saving titles for offline viewing (v2 candidate).
- Chromecast / casting (v2 candidate).
- Picking a source automatically. The user always chooses the magnet link.
- Hosting, mirroring or caching content on any server Jazzy runs. Jazzy runs no content servers.
- iOS, web, Fire OS-specific builds.

## 3. Users and personas

| Persona | Device | Primary need |
|---|---|---|
| **Couch viewer** | Android TV / Google TV with a remote | Pick a profile, find something, pick a well-seeded link, watch. Resume last night's episode in two clicks. |
| **Mobile viewer** | Phone, often on Wi-Fi at home or while travelling | Quick search, watch in landscape, pick up where they stopped. |
| **Parent** | Either | A Kids profile that only shows age-appropriate titles and can't be left without a PIN. |
| **Power user** | Either | Compare sources (seeders, quality, codec, size) before playing. Manage cache and site settings (mirrors, logins for private sites). |

## 4. Information architecture

```
Launch
 └─ Profile picker ("Who's watching?")
     ├─ Add / edit / delete profile (+ optional "pick genres you like")
     └─ Home (per profile)
         ├─ Hero billboard
         ├─ Rails: Continue Watching, Recommended for You, Because you watch <Genre>,
         │         My List, Liked, Trending, Top 10, Popular, Originals, Top Rated,
         │         Upcoming, Now Playing, genre rails
         ├─ Nav: Home · Movies · TV Shows · Discover · My List · Liked · History · Search
         └─ Title Details
             ├─ Movie: info + My List + ♥ Like + Trailer │ Sources list (▶ Play on each row)
             └─ Show:  info + seasons + My List + ♥ Like │ Episodes side panel
                                                  └─ Episode → Sources list (▶ Play on each row)
                                                                   └─ Player (Episodes side panel, Sources)
Library
 ├─ My List (saved to watch later)
 ├─ Liked (grid, movies + shows)
 └─ History (every playback, newest first, with stop time → Resume)
Settings (app-wide; some items per profile)
 ├─ Profiles & parental controls
 ├─ Playback & subtitles
 ├─ Sources (torrent sites)
 ├─ History & recommendations
 ├─ Network, storage & local data
 └─ About / TMDB attribution / licences
```

**Navigation by form factor**
- **Phone:** a bottom navigation bar (Home · Search · Discover · Library). Library has three tabs: **My List**, **Liked** and **History**. Movies and TV appear as filter chips at the top of Home. The profile avatar sits in the top bar.
- **TV:** a left navigation drawer that collapses to icons and expands on focus (Profile, Search, Home, Movies, TV Shows, Discover, My List, Liked, History, Settings).

## 5. Functional requirements

Priorities: **P0** = MVP must-have, **P1** = v1.0 release, **P2** = later.

### 5.1 Profiles (P0)

| ID | Requirement |
|---|---|
| PR-1 | The first launch with no profiles goes straight to **Create profile**. After that, every launch opens **Who's watching?**, unless "Remember my profile" is on for that device. |
| PR-2 | Up to **5 profiles** per device. Each has: name (1–20 chars), avatar (from a bundled set of ≥ 24), maturity level, UI and content language, subtitle language, autoplay-next-episode preference. |
| PR-3 | **Optional genre picks** when a profile is created: "Pick 3 or more genres you like", from the TMDB genre list. These seed recommendations until there's enough watch history (§5.7). They can be skipped and edited later. |
| PR-4 | **Kids profile** flag: limits content to the chosen maturity ceiling (see §6.4), hides the Sources settings, and uses a simpler UI. |
| PR-5 | An optional 4-digit **profile PIN**. A **parental PIN** protects creating, editing and deleting profiles, and leaving a Kids profile. |
| PR-6 | Each profile keeps its own data: My List, Liked list, play history, resume points, Continue Watching, search history, genre affinity and recommendations. |
| PR-7 | Deleting a profile needs confirmation and erases all of that profile's rows from the local database. The last remaining profile can't be deleted. |
| PR-8 | Switching profiles is available from the avatar menu (phone) or the drawer (TV) without restarting the app. |

**Acceptance:** two profiles on one device see separate My Lists, Liked lists, histories, Continue Watching and recommendations. A Kids profile never shows a title above its maturity ceiling in any rail, in search, in recommendations or under More Like This.

### 5.2 Home screen (P0)

Netflix-style layout. Only the layout pattern is borrowed; no Netflix branding, names or assets.

**Hero billboard**
- One featured title, rotated per session. Preference order: a top **Recommended for You** title, then today's Trending titles that have a backdrop and a TMDB **title logo** (`/images`). Without a logo, the title is shown as text.
- Shows: backdrop (with a gradient scrim), title logo, a one-line tagline or the overview cut to 2 lines, genre chips, a maturity badge, and **Watch** (opens Details with focus on the Sources list or Episodes panel), **My List +/✓**, **♥ Like** and **More Info** buttons.
- TV: the billboard fills the top ~60% of the screen. When focus moves to a rail card, the billboard's backdrop and text cross-fade to that card's title.

**Rails**: each rail is a horizontal list that pages in more items as it scrolls (Paging 3, 20 items per TMDB page, up to 5 pages per rail). Every TMDB-backed rail first reads from the local SQLite cache and refreshes in the background when the cache is stale (§6.3).

| Rail | Movies source | TV source | Priority |
|---|---|---|---|
| Continue Watching | local DB (`watch_progress`) | local DB (`watch_progress`) | P0 |
| **Recommended for You** | genre engine (§5.7) → `/discover/movie` | genre engine (§5.7) → `/discover/tv` | P0 |
| **Because you watch *Genre*** (top 2 genres) | `/discover/movie?with_genres=` | `/discover/tv?with_genres=` | P0 |
| **My List** | local DB (`my_list_item`) | local DB (`my_list_item`) | P0 |
| **Liked** | local DB (`liked_title`) | local DB (`liked_title`) | P0 |
| Trending Now | `GET /trending/movie/{day\|week}` | `GET /trending/tv/{day\|week}` | P0 |
| Popular | `GET /movie/popular` | `GET /tv/popular` | P0 |
| Top Rated | `GET /movie/top_rated` | `GET /tv/top_rated` | P0 |
| Upcoming | `GET /movie/upcoming` | `GET /tv/on_the_air` (labelled "New Episodes") | P0 |
| **Originals** (see note) | `GET /discover/movie?with_companies=…` | `GET /discover/tv?with_networks=…` | P0 |
| Now Playing in Theatres | `GET /movie/now_playing` | `GET /tv/airing_today` | P1 |
| Discover picks | `GET /discover/movie` with a curated preset | `GET /discover/tv` with a curated preset | P1 |
| Genre rails (Action, Comedy, Sci-Fi…) | `GET /discover/movie?with_genres=` | `GET /discover/tv?with_genres=` | P1 |
| Top 10 Today (big numbered cards) | first 10 of Trending (day) | first 10 of Trending (day) | P1 |

> **"Originals" note:** Jazzy has no original content of its own, and TMDB has no "originals" endpoint. This rail shows originals from streaming networks via TMDB Discover. It's configurable, with defaults of Netflix (`with_networks=213`), HBO (`49`), Apple TV+ (`2552`), Prime Video (`1024`) and Disney+ (`2739`). The rail can be one mixed row, or one row per network titled "*Network* Originals". The network IDs are stored in the local DB, seeded from a bundled JSON file.

**Card styles**
- Default: a poster card, 2:3 (`w342`), with small markers when the title is in My List (✓) or liked (♥).
- Continue Watching: a backdrop card, 16:9 (`w780`), with a progress bar, "S2:E5" for episodes, and a ⋮ menu (Resume, Episodes & info, My List +/✓, ♥ Like, Mark as watched, Remove from row).
- Top 10: a large rank numeral beside the poster.
- TV focus state: scale 1.1, a 2–3 dp outline, elevation, and the card's title shown under the rail.

**Home behaviour**
| ID | Requirement |
|---|---|
| HM-1 | Rails render **from the cache first**. Skeleton shimmer shows only when a rail has no cached data. A rail that fails with no cache shows "Couldn't load — Retry". Empty rails are hidden. |
| HM-2 | Pull to refresh on phone, or a Settings action on TV, forces a network refresh of all rails. |
| HM-3 | Rail order in v1: Continue Watching (if not empty), Recommended for You, Because you watch *Genre* ×2, My List, Liked, Trending, Top 10, Popular, Originals, Top Rated, Upcoming, genre rails. A profile with no history and no genre picks skips the recommendation rails, so Trending comes first. |
| HM-4 | Movies / TV Shows tabs show the same layout filtered to one media type. |
| HM-5 | Offline: Home shows cached rails with an "Offline" banner. The local rails (Continue Watching, My List, Liked) always work. |
| HM-6 | On TV, Back from deep in the rails scrolls to the top first, then opens the drawer, then asks before exiting. |
| HM-7 | Tapping a Continue Watching card opens Details with the last-used source row marked **▶ Resume 42:13** (§5.3). For a show, the episode in progress is selected. |

### 5.3 Title details and Sources (P0)

Opening any title triggers two things **in parallel**:
1. **Metadata:** shown from the SQLite cache straight away if present, refreshed from `GET /movie/{id}?append_to_response=videos,credits,images,release_dates,external_ids,recommendations,similar` (TV: `content_ratings` instead of `release_dates`, plus `GET /tv/{id}/season/{n}`).
2. **Source search** (§5.8) for the movie or, for a show, the **selected episode**. Results stream into the Sources list as each site answers.

#### 5.3.1 Header (movies and shows)
| ID | Requirement |
|---|---|
| DT-1 | Shows backdrop, title logo or title, year, runtime (movie) or season count (TV), maturity rating for the profile's region, TMDB vote average, genres, overview, top 10 cast, director or creators. |
| DT-2 | Actions: **My List +/✓** (toggles; see §5.6.1), **♥ Like** (toggles; see §5.6.2), **Trailer**, and **More Like This**. There's **no generic Play button**. Playback always starts from a magnet row. |
| DT-3 | **Resume banner:** if the profile has progress on this title or episode, the header shows *"You stopped at 42:13 · 1080p WEB-DL (last used)"*. Selecting it moves focus to that source's row, which offers **▶ Resume 42:13** and **Start over**. |
| DT-4 | **Trailer:** plays the first official YouTube trailer from `videos`. v1 opens the YouTube app or a browser intent. P2: play in-app. |
| DT-5 | **More Like This** grid from `recommendations`, falling back to `similar`. |
| DT-6 | Titles not released yet (future release date) still run the source search, with a warning above the list: *"Not released yet — links may be fake or low quality."* |

#### 5.3.2 Sources list (movies, and episodes once one is selected)
| ID | Requirement |
|---|---|
| SL-1 | Lists **every magnet result with ≥ 1 seeder**, from every enabled site, de-duplicated across sites by info-hash. A "Show dead links (0 seeders)" toggle reveals the rest. |
| SL-2 | **Default sort: seeders, highest first.** Ties are broken by higher resolution, then smaller size. The user can re-sort by Size or Quality, and the choice is remembered per profile. |
| SL-3 | Each row: **▶ Play** button · resolution badge · source badge (WEB-DL, BluRay, CAM…) · codec/HDR · audio · size · **seeders ↑ / leechers ↓** · site name · health dot (green ≥ 50, amber 10–49, red < 10) · the release name (cut short; expands on long-press or focus-hold). |
| SL-4 | Warning badges on the row don't hide it: **CAM/TS** (amber: "low quality"), **may not play on this device** (codec or HDR it can't decode, see §5.8), **season pack**. |
| SL-5 | Filter chips: 2160p · 1080p · 720p · SD · Hide CAM/TS · Hide unsupported. They're off by default, so everything is shown. |
| SL-6 | The row for the source this profile last played gets a **"Last played · stopped at 42:13"** badge and shows **▶ Resume** next to **▶ Play from start**. It stays in its sorted position. |
| SL-7 | A source flagged bad (failed to load or a corrupt file within 24 h) shows a red "Failed recently" badge but stays playable. |
| SL-8 | States: searching (a progress chip per site, with rows appearing as they arrive) · no results (with a "Check sites" shortcut and a "Search with original title" retry) · per-site errors, inline and collapsible, with the reason shown (timeout, blocked by the site, site unreachable) · all sites disabled (links to Settings → Sources). |
| SL-9 | Results are cached in SQLite for 15 min per (title, episode). Coming back to Details within that time shows the cached list right away and refreshes seed counts in the background. Pull to refresh (phone) or a **Refresh** button (TV) re-queries now. |

#### 5.3.3 Layout by form factor
| Context | Layout |
|---|---|
| **TV — movie** | Two panes. **Left (~40%):** backdrop-faded info, Like, Trailer, resume banner. **Right (~60%):** the Sources list, which gets initial focus on its first row (or on the Resume row if there's progress). |
| **TV — show** | Netflix-style two panes. **Left:** show info, Like, and a vertical **season list** (Season 1…N, Specials last). **Right: Episodes side panel**, with one row per episode of the focused season: still, "3. Episode title", runtime, two-line overview, and a watched ✓ or progress bar. Focus starts on the next episode to watch. Pressing an episode **slides in its Sources list** over the right pane (the episode list narrows to a strip). Back returns to the episodes. |
| **Phone portrait — movie** | Header at the top, then a **Sources** section below it as a scrolling list. A sticky mini-header shows the title and ♥ while scrolling. |
| **Phone portrait — show** | Header, a season dropdown, then the episode list. Tapping an episode opens its Sources list in a **bottom sheet** that's 85% high and can be dragged to full height. |
| **Phone landscape / tablet (expanded width)** | Same as TV: info on the left, **episodes side panel** or Sources list on the right. For shows, an episode's Sources replaces the right pane, with a back arrow. |

### 5.4 Search (P0)

| ID | Requirement |
|---|---|
| SR-1 | One search box using `GET /search/multi` (movies + TV; person results open a filmography grid). The input waits 300 ms after typing stops (debounce), needs ≥ 2 characters, and pages results. Search responses are cached in SQLite for 1 h. |
| SR-2 | Phone: the system keyboard plus voice input. TV: an on-screen keyboard plus **voice search** via the remote mic. |
| SR-3 | Before typing: recent searches (per profile, stored in SQLite, clearable) and "Popular searches" (Trending). |
| SR-4 | Results show as a poster grid with filter chips (All · Movies · TV · People). Selecting a result opens Details (§5.3), which immediately starts the source search. |
| SR-5 | Results respect the profile's maturity filter and `include_adult=false`. |
| SR-6 | P1: the app answers Google TV global search through a `SearchManager` content provider. |

### 5.5 Discover (P1)

A filter screen backed by `GET /discover/movie` and `GET /discover/tv`.

- Filters: media type, genres (multi-select, from the cached TMDB genre lists), release year range, minimum rating, minimum vote count (default 100), original language, runtime range, "available to stream in my region" (P2).
- Sort: Popularity, Rating, Release date, Revenue (movies), Title.
- Results grid with infinite scroll. P2: save a filter set as a custom Home rail.

### 5.6 Library: My List, Liked, History and resume (P0)

#### 5.6.1 My List
My List is the "save to watch later" list. It's separate from Liked, which records titles the user enjoyed.

| ID | Requirement |
|---|---|
| ML-1 | A **My List +/✓** toggle on Details, the hero billboard, the card menu (long-press on phone, menu button on TV) and the player's top bar. Adding a show adds the whole show. |
| ML-2 | Saved straight to SQLite (`my_list_item`) with a title snapshot (§7), so My List shows offline. |
| ML-3 | **My List screen** (Library → My List on phone, the drawer on TV): a poster grid, most recently added first, with filter chips All · Movies · TV Shows and sort options (Date added, Title, Release year). |
| ML-4 | Removing from the grid (card menu) gives a 5 s **Undo** snackbar. |
| ML-5 | A setting, **"Remove from My List when watched"** (default off), removes a movie when it's watched (RS-3), or a show when its last aired episode is watched. |
| ML-6 | **My List** appears as a Home rail (§5.2), and titles in it count as a mild signal for recommendations (§5.7). |

#### 5.6.2 Like and the Liked list
| ID | Requirement |
|---|---|
| LK-1 | A **♥ Like** toggle on Details, the hero billboard, the card menu (long-press on phone, menu button on TV) and the player's top bar. Liking a show likes the whole show, not single episodes. |
| LK-2 | A like is saved straight to SQLite (`liked_title`) with a title snapshot (§7), so the Liked list shows offline and doesn't depend on the TMDB cache. |
| LK-3 | **Liked screen** (Library → Liked on phone, the drawer on TV): a poster grid, newest likes first, with filter chips All · Movies · TV Shows and sort options (Date liked, Title, Release year). |
| LK-4 | Unlike from the grid (card menu) gives a 5 s **Undo** snackbar. |
| LK-5 | Likes count as a strong signal for recommendations (§5.7). |
| LK-6 | **Liked** also appears as a Home rail (§5.2). |

#### 5.6.3 Resume points
| ID | Requirement |
|---|---|
| RS-1 | One resume point per (profile, title) for movies and per (profile, show, season, episode) for episodes. It stores **position, duration, last source (info-hash + file index + release name) and an updated timestamp**. |
| RS-2 | The position is written to SQLite **every 10 s during playback** and right away on pause, seek, source switch, `onStop` and player close, so a crash or a killed process loses at most 10 s. |
| RS-3 | A title or episode counts as **watched** at ≥ 90% (or at the start of the end credits, if detected). It then leaves Continue Watching. For shows, the **next episode** takes its place with position 0. |
| RS-4 | Continue Watching shows items with ≥ 2 min **and** ≥ 3% progress that aren't watched yet, newest first, capped at 20. |
| RS-5 | Resume works with **any** source, because the position is stored as time, not bytes. If the user picks a different source, they're asked "Resume from 42:13 / Start over". |
| RS-6 | "Mark as watched / unwatched" on cards and episode rows updates the resume point without creating a history entry. |

#### 5.6.4 Play history
| ID | Requirement |
|---|---|
| HS-1 | **Every playback session** is recorded in `play_session`, whether it ran 5 s or 3 h. Each session stores: profile, title, season and episode, source (info-hash, release name, quality, site), **start and stop time, start position, stop position**, duration, watched ms, and the end reason (`user_exit`, `completed`, `next_episode`, `source_switch`, `error`, `stalled`). |
| HS-2 | Sessions where less than 10 s was watched are kept in the database but hidden from the History screen. |
| HS-3 | **History screen** (Library → History on phone, the drawer on TV): newest first, **grouped by day** (Today, Yesterday, This week, then by date). Each entry shows the poster or episode still, title (with "S1:E3 · Episode title"), *"Stopped at 42:13 of 1:58:00"*, a progress bar, the time watched and the source quality. |
| HS-4 | Selecting a history entry opens Details with that episode selected and that source's row marked **▶ Resume** at the stored stop position. |
| HS-5 | Several sessions of the same title on the same day are folded into one entry ("Watched 3 times"), which can be expanded. |
| HS-6 | Remove a single entry (swipe on phone, menu on TV), remove all entries for a title, or **Clear all history**, which needs the profile PIN on a protected profile. Clearing history also resets that profile's genre affinity, after a confirmation. |
| HS-7 | Search history filtered by title (phone: a search icon in History; TV: P1). |
| HS-8 | History is never uploaded anywhere. It's stored only in the local database (§7). |

### 5.7 Recommendations from watch history (P0)

Suggestions are computed **on the device** from the profile's history and likes, scored against **TMDB's genre lists**.

**Inputs**
- **Genre lists:** `GET /genre/movie/list` and `GET /genre/tv/list`, cached in SQLite (`genre`) for 7 days in the profile's language.
- **Play history** (`play_session` + `watch_progress`) and the genre IDs of each title (from the title snapshot).
- **Likes** (`liked_title`), the strongest signal.
- **My List** (`my_list_item`), a mild signal: the user intends to watch these.
- **Seed genres** chosen at profile creation (PR-3).

**Movie ↔ TV genre mapping**: TMDB uses different IDs for some genres on movies and TV. Jazzy scores genres in one **unified genre space** using a bundled mapping:

| Unified | Movie IDs | TV IDs |
|---|---|---|
| Action & Adventure | 28 Action, 12 Adventure | 10759 Action & Adventure |
| Sci-Fi & Fantasy | 878 Science Fiction, 14 Fantasy | 10765 Sci-Fi & Fantasy |
| War & Politics | 10752 War | 10768 War & Politics |
| Kids & Family | 10751 Family | 10751 Family, 10762 Kids |
| Same ID on both | 16 Animation, 35 Comedy, 80 Crime, 99 Documentary, 18 Drama, 9648 Mystery, 37 Western | same |
| Movie-only / TV-only | 36 History, 27 Horror, 10402 Music, 10749 Romance, 53 Thriller, 10770 TV Movie | 10763 News, 10764 Reality, 10766 Soap, 10767 Talk |

When querying, each unified genre expands back into the right IDs for the media type. Movie-only genres map to their nearest TV genre: Horror and Thriller go to Mystery, Drama and Crime; Romance goes to Drama; History goes to Documentary and War & Politics.

**Scoring** (rerun after every playback session, every like or unlike, every My List change, and on app start if older than 12 h)
```
for each title the profile has played (latest session per title) or liked:
    engagement = completion ≥ 90% → 1.0 | 50–90% → 0.7 | 10–50% → 0.4 | < 10% → 0.1
                 (a show's completion = the average across its watched episodes, capped at 1.0)
    recency    = 0.5 ^ (days_since_last_played / 30)          // 30-day half-life
    w          = engagement × recency
    if liked:  w += 1.5 × 0.5 ^ (days_since_liked / 90)       // likes fade more slowly
for each title in My List and never played:
    w = 0.5 × 0.5 ^ (days_since_added / 60)
    for each unified genre g of the title:
        affinity[g] += w / sqrt(number_of_genres_on_title)    // a title with many genres counts less per genre
seed genres (PR-3): affinity[g] += 0.8, fading to 0 over the first 10 sessions
normalise affinity to 0..1, store in genre_affinity
```

**Building the rails**
| Rail | Query | Re-rank and filter |
|---|---|---|
| **Recommended for You** | For each media type, `/discover/{type}?with_genres={top3 joined by "\|"}&sort_by=popularity.desc&vote_count.gte=200&vote_average.gte=6.5`, pages 1–3. The movie/TV split matches the profile's history split (min 30% of each, if the profile has watched both). | `score = Σ affinity[g] over the item's genres × (vote_average / 10)`. **Exclude** watched titles (≥ 90%), titles already in Continue Watching, and titles played in the last 30 days. Apply the maturity filter. Keep the top 40. |
| **Because you watch *Genre*** (top 2 genres) | `/discover/{type}?with_genres={g}&sort_by=vote_average.desc&vote_count.gte=500` | Same exclusions. Not to repeat more than 10 items already in Recommended for You. |

- Results are cached in SQLite (`recommendation_cache`) per profile and rail, so Home loads them instantly. They're rebuilt in the background on the triggers above.
- **Cold start:** with no history, no likes, an empty My List and no seed genres, the recommendation rails are hidden. Once at least one session ≥ 10 min, one like or one My List item exists, they appear.
- Settings → History & recommendations: "Reset recommendations" (clears `genre_affinity` and the cache) and "Edit favourite genres".

### 5.8 Source search (P0)

```
Details opened (movie) / episode selected (show)
  → Check source_result_cache (15 min TTL) → render cached rows immediately if present
  → Build query from TMDB data (title, year, IMDb ID, season/episode)
  → Query all enabled sites in parallel (timeout 15 s each; rows render as each site answers)
  → Normalise + de-duplicate across sites (by info-hash, keep the highest seeder count)
  → Parse release names (quality, codec, source, size, HDR, audio)
  → Drop results that clearly belong to a different title / year / episode
  → Sort by seeders, highest first → Sources list (§5.3.2), ▶ Play on each row
  → Save to source_result_cache
```

**Provider model: built-in torrent-site providers**

Jazzy searches torrent sites directly. It reuses the search engine from the `TorrentSearch` project (`../TorrentSearch`), a Kotlin port of *torrent-search-api* (MIT). Each site is a declarative provider definition: a search URL template, category mapping, row selectors and field selectors. Sites with a JSON API get a small subclass. Users don't install or configure anything for sources to appear.

| ID | Requirement |
|---|---|
| SRC-1 | Every source sits behind the `TorrentProvider` interface from the TorrentSearch engine (`search(query, category)` → torrents with title, seeds, peers, size, magnet or detail link). It's ported into `:core:sources` as pure Kotlin (no Android imports), with OkHttp and Jsoup. |
| SRC-2 | **Built-in public providers, enabled by default.** These worked in TorrentSearch's live check on 2026-10-10: **ThePirateBay** (apibay JSON), **YTS** (JSON, movies only, one result per quality), **LimeTorrents**, **TorrentProject**, **KickassTorrents** (`kickasstorrents.cc` mirror) and **MagnetDL**. Providers that are blocked or dead at release time (1337x and EZTV behind Cloudflare challenges, Torrent9, Torrentz2, RARBG) ship **disabled** and can be enabled from Settings if a working mirror exists. |
| SRC-3 | **Private trackers (optional):** IPTorrents, TorrentLeech and YggTorrent are disabled by default and enabled by entering credentials or cookies (Settings → Sources). Credentials and cookies are encrypted with an Android Keystore key. |
| SRC-4 | **Mirrors:** every provider's base URL can be overridden in Settings, because domains move often. Mirror domains must be checked before they're adopted. Some clones serve the **same unrelated magnet for every result** (bait and switch), have no magnets, or redirect to ad pages. TorrentSearch's notes list the mirrors already checked. |
| SRC-5 | **Per-site settings and health:** enable/disable toggle, mirror URL, login/cookies (private sites), health (last success, average latency, error count, last failure reason) and a **Test** button. A site that keeps failing is marked "Not responding" in the Sources list. It's never disabled silently. |
| SRC-6 | **No access-control circumvention.** Jazzy doesn't solve CAPTCHAs or Cloudflare challenges. A challenge response is reported as "Blocked by the site", and that site is skipped for the search. This is the same rule as in TorrentSearch. |
| SRC-7 | **Lazy magnet resolution:** some sites (e.g. KickassTorrents, 1337x) don't list magnets in search results. Their rows show seeds and size from the listing. The magnet is scraped from the detail page when the user presses **▶ Play**, with a short "Getting link…" state (≤ 5 s, then an error with a retry). Resolved magnets are cached with the source result. |
| SRC-8 | **Cross-site de-duplication:** rows with the same info-hash (from the magnet, or a hash field in the listing) merge into one row that shows the highest seed count and every site name. Rows whose magnet isn't resolved yet de-duplicate by normalised title + size (±1%). |
| SRC-9 | **P1: Torznab provider** for users who also run Jackett or Prowlarr. It's an optional extra provider, with ID-based search (`t=movie&imdbid=`, `t=tvsearch&imdbid=&season=&ep=`). |
| SRC-10 | **P1 (v1.1): remote provider definitions.** The definitions are declarative, so updated base URLs and selectors can ship as a signed JSON bundle checked at app start. A site layout change can then be fixed without an app release. |

**Query building**
- Most sites only support free-text search, so the query is built from TMDB data and results are checked against the title and year (see *Result parsing*). Each provider's **category** is set to its Movies or TV category, where the site has one.
- Movie: `"{title} {year}"`, plus a second query with the `original_title` if it differs. **YTS** is queried with the IMDb ID (from `external_ids`) where its API accepts it, which is exact.
- Episode: `"{show} S{ss}E{ee}"`, then season packs (`"{show} S{ss}"`), then complete-series packs (`"{show} complete"`). YTS is skipped for TV. Packs are listed with a **season pack** badge, and Play opens the matching episode file inside the pack.
- Strip punctuation and normalise `&`/`and`, accents and leading articles before matching.

**Result parsing**
| Field | Parsed from | Used for |
|---|---|---|
| Seeders / leechers | site listing | **primary sort**; health dot |
| Resolution | `2160p`, `1080p`, `720p`, `480p` | badge, filter chip, tie-break |
| Source | `BluRay`, `WEB-DL`, `WEBRip`, `HDTV`, `CAM`, `TS`, `TC`, `HDCAM` | badge, with an amber warning for CAM/TS/TC |
| Video codec | `x264`/`AVC`, `x265`/`HEVC`, `AV1` | "may not play" badge via a `MediaCodecList` check |
| HDR | `HDR10`, `HDR10+`, `DV` | "may not play" badge on devices without HDR/DV decode |
| Audio | `AAC`, `AC3`, `DDP`/`EAC3`, `DTS`, `Atmos`, channels | badge |
| Size | site listing or magnet | shown, sort option |
| Year / S/E | name | match check (year within ±1 of TMDB, correct episode or a pack that contains it) |

### 5.9 Streaming engine and player (P0)

#### 5.9.1 Torrent streaming engine (`core:torrent`)

| ID | Requirement |
|---|---|
| TE-1 | Built on **libtorrent** through JNI bindings (**libtorrent4j**), shipping `arm64-v8a`, `armeabi-v7a` and `x86_64` libraries split per ABI. |
| TE-2 | **Magnet → metadata:** add the magnet with DHT, PEX, LSD and the trackers it lists, plus a configurable list of public trackers stored in the DB. Metadata timeout 30 s, then *"This link isn't responding"*, offering the next row in the Sources list. **Magnet verification:** once metadata arrives, the torrent's name and file names are checked against the expected title (and episode). A clear mismatch, such as a bait-and-switch mirror serving the same magnet for every result, stops playback, flags the source (`source_flag`) and offers the next row. |
| TE-3 | **File selection:** for movies, the largest video file (`.mkv`, `.mp4`, `.avi`, `.m4v`, `.webm`, `.ts`). For episodes, the file whose parsed `SxxEyy` matches (this handles season packs). Every other file gets `priority = 0`. Samples (`sample` in the name, or < 100 MB) are skipped. |
| TE-4 | **Downloading from many peers at once:** the engine connects to many peers in parallel (default max 200 connections, configurable) and requests different pieces from each. A **playback-aware piece scheduler** runs instead of plain sequential mode:<br>• **Head:** the first ~5 MB and the **tail:** the last ~5 MB get the highest priority with immediate deadlines, because container indexes such as MP4 `moov` or MKV cues are often at the end.<br>• **Read-ahead window:** pieces covering `[playhead, playhead + max(30 s × bitrate, 20 MB)]` get deadlines set with `set_piece_deadline`, nearest first. The window grows as the download rate allows.<br>• **Background:** remaining pieces at normal priority. |
| TE-5 | **Seeking:** a read at byte offset *o* is mapped to piece index `o / piece_length`. The engine reprioritises a new read-ahead window from there and lowers the old window's priority. |
| TE-6 | **Blocking reads:** reading bytes that aren't downloaded yet waits on a piece-finished alert, with a timeout. The player shows a buffering state with live stats. |
| TE-7 | **Stall detection:** if the download rate stays below the stream bitrate × 1.1 for 20 s and the buffer is < 5 s, show a non-blocking prompt: *"This link is slow — try the next one?"* The prompt names the next row in the Sources list (by seeders) and keeps the playback position. The switch is recorded as a history session with end reason `stalled`. |
| TE-8 | **Engine lifecycle:** runs inside a foreground service (`foregroundServiceType="mediaPlayback"`) while a stream is active. When playback ends, the torrent is paused or removed according to the seeding policy. |
| TE-9 | **Storage:** stream data is written to app-specific storage, with a user-set cache cap (default 10 GB on TV, 4 GB on phone) and LRU eviction (`torrent_cache_entry` table). One-tap clear. |
| TE-10 | **Upload and seeding policy:** upload rate limit (default 1 MB/s on Wi-Fi, 0 on cellular), "Stop seeding when playback stops" (default ON), and a ratio cap. |
| TE-11 | **Network rules:** a "Wi-Fi/Ethernet only" toggle (default ON on phone), a warning before streaming on a metered connection, and a configurable incoming port, UPnP/NAT-PMP and encryption mode. |
| TE-12 | Live stats exposed to the UI: download/upload rate, peers/seeds connected, % of the selected file, buffered-ahead seconds. |

#### 5.9.2 Bridge from the engine to the player
- **Primary:** a custom Media3 `DataSource` (`TorrentDataSource`) reads straight from the engine's file handle, honouring `DataSpec.position`/`length`, and waits on TE-6 for missing pieces.
- **Secondary (P1):** a loopback HTTP server (`127.0.0.1`, random port and path token, `Range` support) for **"Open in external player"**.

#### 5.9.3 Player (`feature:player`, Media3 ExoPlayer)

| ID | Requirement |
|---|---|
| PL-1 | Media3 ExoPlayer with the default extractors (Matroska, MP4 and others). HEVC/AV1 hardware decode where the device supports it. |
| PL-2 | **Audio coverage:** the device decodes AAC, AC3 and EAC3 directly or via HDMI passthrough. **DTS/TrueHD:** passthrough when supported, otherwise the Media3 **FFmpeg decoder extension** (built in-house, LGPL configuration). |
| PL-3 | **Pre-roll screen** while buffering to start: the backdrop and title, plus "Connecting to peers… 34 peers · 4.2 MB/s · 62%". A cancel button returns to Details. Cancelling still writes a `play_session` row (end reason `user_exit`). |
| PL-4 | Controls: play/pause, seek ±10 s, a scrub bar showing buffered ranges, audio and subtitle pickers, playback speed (0.5–2×), aspect/zoom, **♥ Like**, **Sources** (opens this title's or episode's Sources list as a side panel; switching keeps the position), a stats overlay and, for shows, **Episodes**. |
| PL-5 | **Episodes side panel (shows):** a panel that slides in from the right (TV, and phone landscape) listing the current season's episodes, with a season switcher at the top. The current episode is highlighted, and watched ✓ and progress marks are shown. Choosing another episode opens its Sources list in the same panel. Playback keeps running behind the panel until a source is chosen. |
| PL-6 | TV remote: D-pad left/right = seek ±10 s (holding speeds up), centre = play/pause and show controls, up = tracks/settings row, down = episodes strip / Sources. Media keys are supported. |
| PL-7 | Phone: double-tap left/right to seek, vertical swipe for brightness and volume, pinch to zoom, landscape lock, **Picture-in-Picture**. |
| PL-8 | **Subtitles:** embedded tracks (SRT, SSA/ASS with basic styling, PGS where supported) and sidecar `.srt`/`.vtt` files inside the torrent. **P1:** OpenSubtitles.com search by IMDb ID plus language, with a ±sync offset control. Subtitle style settings. The last chosen subtitle and audio language is remembered per profile in the DB. |
| PL-9 | **Resume and history:** see RS-1…RS-6 and HS-1. Opening a source with a stored position prompts "Resume from 42:13 / Start over" unless the user already chose Resume or Start over on the row. |
| PL-10 | **Next episode:** a countdown 10 s before the end credits (or at 95%). If the current torrent is a **season pack** containing the next episode, the next episode plays from the same torrent automatically (if autoplay is on). Otherwise the next episode's Sources list (prefetched at 80% progress) appears in the side panel with the top row focused, and the user presses Play. |
| PL-11 | Media session integration: lock-screen and notification controls on phone, and Google TV "Now Playing". |
| PL-12 | Errors: unsupported codec → open the Sources panel with "Hide unsupported" switched on. Corrupt file → mark the source as bad for 24 h (`source_flag`), record the session with end reason `error`, and open the Sources panel. |

### 5.10 Android TV specifics (P0 unless noted)

- One APK. The manifest declares `<uses-feature android:name="android.software.leanback" android:required="false"/>` and `android.hardware.touchscreen required="false"`, has a `LEANBACK_LAUNCHER` intent filter and a **320×180 banner**.
- UI: **Compose for TV** (`androidx.tv:tv-material`).
- Focus: every screen opens with initial focus on a sensible element (on Details: the Resume row, else the first source row or the next episode), focus is restored when returning to a screen, and focus never gets trapped.
- Overscan-safe padding: 48 dp horizontal, 27 dp vertical.
- **P1:** publish Continue Watching and next episodes to the Google TV **Watch Next** row via `androidx.tvprovider`, using the local `watch_progress` table.
- **P2:** a home-screen channel ("Jazzy Recommended").

### 5.11 Settings

| Section | Items |
|---|---|
| Profiles | Manage profiles, parental PIN, Kids profile ceiling, favourite genres |
| Playback | Autoplay next episode, default audio language, default Sources sort, default filter chips |
| Subtitles | Default language, style, OpenSubtitles login (P1) |
| Sources | Torrent sites: enable/disable, mirror URL, login/cookies for private sites, health and Test · Torznab endpoints (P1) · public tracker list · show dead links by default |
| History & recommendations | Remove from My List when watched, clear play history, clear search history, reset recommendations, edit favourite genres |
| Network & storage | Wi-Fi only, upload limit, seeding policy, max connections, port, torrent cache size, clear torrent cache |
| Local data | Size of the app data DB and the cache DB, **Clear TMDB cache** (keeps history, My List, likes and resume points), export/import profile data as JSON (P2) |
| About | Version, open-source licences, **TMDB attribution and logo**, privacy notice, disclaimer (§11) |

All settings are stored in the local SQLite database (§7). There's no separate preferences store.

## 6. TMDB integration details

### 6.1 Auth and config
- Uses the TMDB **API Read Access Token** (v4 bearer) for v3 endpoints, sent as `Authorization: Bearer <token>`.
- The token is injected at build time from `local.properties` / CI secrets into `BuildConfig`, and never committed. Any token shipped in an APK can be extracted. That's acceptable for v1. P2: move the calls behind a thin proxy if needed.
- `GET /configuration` (cached in SQLite for 7 days) supplies the image base URL and sizes. Default sizes: posters `w342` (phone) / `w500` (TV), backdrops `w780` / `w1280`, stills `w300`, logos `w500`.

### 6.2 Locale
- `language` comes from the profile. `region` comes from the device locale.
- Images: `include_image_language={lang},en,null`.

### 6.3 Local TMDB cache (SQLite)
Recent TMDB fetches are stored in the **cache database** (§7). Reads are **cache-first, then revalidate**:

1. Read from SQLite and emit right away if a row exists, even a stale one.
2. If the row is missing or older than its TTL, fetch from TMDB, upsert, and emit again. Room's `Flow` updates the UI.
3. Offline or on an error: keep the stale data and show a subtle "Offline" or "Couldn't refresh" banner.

| Data | Table | TTL |
|---|---|---|
| List / rail pages (popular, trending…) | `tmdb_list_page` | 6 h (trending/day: 2 h) |
| Movie / show details (`append_to_response` payload) | `tmdb_title` | 24 h |
| Seasons + episodes | `tmdb_season` | 24 h (6 h for shows still airing) |
| Search responses | `tmdb_search` | 1 h |
| Discover responses (incl. recommendation queries) | `tmdb_list_page` | 12 h |
| Configuration, genre lists | `tmdb_config`, `genre` | 7 days |

- **Size limit:** the cache DB is capped (default 150 MB). Eviction is LRU on `last_accessed_at`, and also removes rows not accessed for 30 days. Titles referenced by likes, history or resume points keep a lightweight **title snapshot** in the app DB, so those screens never depend on the cache.
- **Images** aren't stored in SQLite. They use Coil's disk cache (default 300 MB TV, 150 MB phone).
- **Rate limits:** stay well below TMDB's ~50 requests/s. At most 8 concurrent requests. On HTTP 429, retry with exponential backoff and honour `Retry-After`.

### 6.4 Maturity filtering
- **Movies:** `certification_country={region}&certification.lte={ceiling}&include_adult=false` on Discover. List endpoints are post-filtered with `release_dates` (cached in `tmdb_title`). Titles with no certification are hidden on Kids profiles.
- **TV:** Kids profiles use `with_genres=10762|10751|16` and post-filter with `/tv/{id}/content_ratings`.
- Ceilings by region map, e.g. US: G, PG, PG-13, R, NC-17 / TV-Y, TV-Y7, TV-G, TV-PG, TV-14, TV-MA.

## 7. Local database (SQLite via Room)

Jazzy uses **two SQLite database files**. Both are managed by Room, use WAL mode, are written off the main thread, and export their schemas so migrations can be tested.

| File | Holds | Android Auto Backup | Can be cleared by user |
|---|---|---|---|
| `jazzy.db` (**app data**) | profiles, settings, My List, likes, resume points, play history, search history, site settings, recommendation state, title snapshots | **included** | only through specific actions (clear history, delete profile) |
| `jazzy_cache.db` (**cache**) | TMDB responses, source search results, recommendation results, torrent cache index | excluded | yes, one tap: "Clear TMDB cache" |

Splitting the files means clearing the cache, or a cache corruption, can never touch history, My List or likes, and backups stay small.

### 7.1 `jazzy.db` — app data
```
profile(id PK, name, avatar_id, is_kids, maturity_ceiling, language, subtitle_language,
        audio_language, pin_hash?, autoplay_next, created_at)

app_setting(key PK, value)                                  // app-wide settings, active_profile_id
profile_setting(profile_id, key, value)                     PK(profile_id, key)

title_snapshot(tmdb_id, media_type, title, original_title, year, poster_path,
               backdrop_path, genre_ids, imdb_id?, updated_at)          PK(tmdb_id, media_type)
               // minimal copy kept for every title that's in My List, liked, in history or has a resume point

my_list_item(profile_id, tmdb_id, media_type, added_at)               PK(profile_id, tmdb_id, media_type)
liked_title(profile_id, tmdb_id, media_type, liked_at)                 PK(profile_id, tmdb_id, media_type)

watch_progress(profile_id, tmdb_id, media_type, season, episode,        // season/episode = -1 for movies
               position_ms, duration_ms, completed, last_info_hash, last_file_index,
               last_source_name, last_quality, updated_at)
               PK(profile_id, tmdb_id, media_type, season, episode)

play_session(id PK, profile_id, tmdb_id, media_type, season, episode, episode_title,
             info_hash, file_index, source_name, quality, provider_id,
             started_at, ended_at, start_position_ms, stop_position_ms, duration_ms,
             watched_ms, end_reason)
             INDEX(profile_id, started_at DESC), INDEX(profile_id, tmdb_id)

search_history(profile_id, query, searched_at)
seed_genre(profile_id, unified_genre_id)
genre_affinity(profile_id, unified_genre_id, score, updated_at)        PK(profile_id, unified_genre_id)

provider_setting(provider_id PK, enabled, base_url_override?, username?, password_encrypted?,
                 cookies_encrypted?, last_ok_at, avg_latency_ms, error_count, last_error)
                 // provider_id = built-in site ("thepiratebay", "yts", …) or "torznab:<uuid>" (P1)
source_flag(info_hash PK, bad_until, reason)
originals_network(network_id PK, name, media_type, enabled, sort_order)
public_tracker(url PK, enabled)
```

### 7.2 `jazzy_cache.db` — cache
```
tmdb_config(key PK, json, fetched_at)
genre(id, media_type, language, name, fetched_at)                       PK(id, media_type, language)
tmdb_list_page(list_key, page, language, region, item_ids, json, fetched_at, last_accessed_at)
tmdb_title(tmdb_id, media_type, language, json, fetched_at, last_accessed_at)
tmdb_season(tmdb_id, season, language, json, fetched_at, last_accessed_at)
tmdb_search(query_key, page, json, fetched_at)
source_result(query_key, info_hash, magnet, name, size_bytes, seeders, leechers,
              provider_ids, desc_url?, parsed_json, fetched_at)            // 15 min TTL; magnet resolved lazily (SRC-7)
recommendation_cache(profile_id, rail_key, position, tmdb_id, media_type, score, generated_at)
torrent_cache_entry(info_hash PK, save_path, size_bytes, completed_fraction, last_accessed_at)
```

- Private-site passwords and cookies (and Torznab API keys, P1) are encrypted with an Android Keystore key before they're written.
- PINs are stored as salted hashes (PBKDF2), never in plain text.
- A schema change to `jazzy.db` **must** ship a tested migration. Destructive migration is only allowed on `jazzy_cache.db`.

## 8. Architecture and tech stack

**Pattern:** MVVM with unidirectional data flow (ViewModel → `StateFlow<UiState>`), repositories that put the local database first (the DB is the source of truth, the network only fills it), and Hilt for dependency injection.

**Modules**
```
:app                      (single APK; phone + TV entry points, nav graphs)
:core:model               (domain models, unified genre mapping)
:core:common              (dispatchers, result types, utils)
:core:network             (TMDB API client, OkHttp setup)
:core:sources             (torrent-site providers ported from TorrentSearch: selector engine, provider
                           definitions, progressive aggregation, magnet resolution; Torznab provider P1)
:core:database            (Room: AppDatabase = jazzy.db, CacheDatabase = jazzy_cache.db, DAOs, migrations)
:core:data                (repositories: Catalog, Title, Search, Profile, Library [My List, likes, history, progress],
                           Sources, Settings)
:core:recommendations     (genre affinity scoring, rail builders)
:core:torrent             (libtorrent4j wrapper, piece scheduler, TorrentDataSource, loopback server)
:core:player              (ExoPlayer factory, renderers, FFmpeg ext, track selection)
:core:designsystem        (phone M3 theme + TV theme, shared tokens, cards, source row, episode row)
:feature:profiles
:feature:home
:feature:details          (header, Sources list, episodes side panel)
:feature:search
:feature:discover
:feature:library          (My List, Liked, History)
:feature:player           (incl. episodes + sources side panels)
:feature:settings
```
Each feature module puts its form-factor-specific screens in `ui/phone` and `ui/tv`, which share the same ViewModel and UiState.

**Stack**
| Concern | Choice |
|---|---|
| Language / build | Kotlin, Gradle version catalog, convention plugins, KSP |
| UI phone | Jetpack Compose + Material 3 + adaptive layouts (window size classes, list-detail panes for episodes) |
| UI TV | Compose for TV (`androidx.tv:tv-material`) |
| Navigation | Navigation 3 (or Navigation Compose) with type-safe routes |
| DI | Hilt |
| Async | Coroutines + Flow |
| Networking | Retrofit + OkHttp + kotlinx.serialization. **Jsoup** for HTML scraping of torrent sites. |
| Paging | Paging 3 with `RemoteMediator` (SQLite cache + TMDB) |
| Images | Coil 3 (disk cache; palette extraction for hero gradients) |
| Persistence | **SQLite via Room** (two databases, §7). No DataStore or SharedPreferences. |
| Background | WorkManager: cache eviction, rebuilding recommendations, Watch Next sync |
| Torrent | libtorrent4j (libtorrent 2.x JNI) |
| Player | AndroidX Media3 (ExoPlayer, Session, UI) + FFmpeg decoder extension |
| TV integration | `androidx.tvprovider` (Watch Next) |
| Testing | JUnit, Turbine, MockWebServer, Room in-memory DB + `MigrationTestHelper`, Robolectric, Compose UI tests, Macrobenchmark + Baseline Profiles |

**Changes to the current project scaffold** (`com.example.jazzy`, AGP 9.4.1, compile/target SDK 37, min SDK 34):
1. Add Compose, Hilt, KSP, Room, Media3, Coil, Paging, WorkManager and libtorrent4j to `libs.versions.toml`.
2. Raise Java compatibility from 11 to **17**.
3. Replace `com.example.jazzy` with a real application ID before any release.
4. Drop AppCompat and Material Components (Views) once Compose is in place.
5. Set up ABI splits. Add R8 rules for libtorrent4j, Media3 and kotlinx.serialization.
6. Add `backup_rules.xml` / `data_extraction_rules.xml` that include `jazzy.db` and exclude `jazzy_cache.db` and the torrent cache.

## 9. Non-functional requirements

| Area | Target |
|---|---|
| **Details → first source row visible** | ≤ 3 s (p50) from a cached result or the fastest site. All sites return or time out within 15 s. **Sources found:** ≥ 90% of popular titles (TMDB Popular, last 5 years) return at least one row with ≥ 10 seeders. |
| **Time to first frame** | ≤ 15 s (p50) after Play on a source with ≥ 50 seeders and a 50 Mbps connection. ≤ 30 s (p90). |
| **Rebuffering** | < 1 rebuffer per 30 min (p50) on sources with ≥ 50 seeders at ≤ 1080p. |
| **Seek latency** | ≤ 5 s to resume after seeking to an undownloaded position (healthy source). |
| **Resume accuracy** | At most 10 s of progress lost on crash or process death. No loss on a normal exit. |
| **Cold start → Home rendered** | ≤ 1.5 s from the SQLite cache on a mid-range phone, ≤ 2.5 s on a Chromecast with Google TV. |
| **DB performance** | All DB access off the main thread. History, My List and Liked screens with 10,000 sessions / 1,000 items scroll at 60 fps (paged queries). |
| **Scrolling** | Rails hold 60 fps on phone. On TV, < 5% janky frames on a Chromecast with Google TV (HD). |
| **Memory** | Player + engine < 400 MB on TV devices with 2 GB RAM. The engine's piece cache is capped (default 64 MB). |
| **Storage** | Torrent cache never exceeds its cap. Cache DB ≤ 150 MB. Warns at < 2 GB free. |
| **Battery (phone)** | No engine activity after playback stops, unless seeding is explicitly on. |
| **Accessibility** | TalkBack labels on all cards, source rows ("1080p WEB-DL, 2.1 GB, 1,240 seeders, Play") and controls. D-pad and switch-access focus order. 200% font scale without clipping. |
| **Localisation** | UI strings fully externalised. v1 in English. Content and genre names follow the TMDB language. |
| **Privacy** | No account, no analytics by default. Opt-in crash reporting. History, likes and recommendations never leave the device. |

## 10. Analytics and success metrics (opt-in, privacy-preserving, aggregate only)

- **Activation:** % of installs that create a profile and finish one playback ≥ 5 min.
- **Playback success rate:** plays that reach the first frame ÷ Play presses on source rows. Target ≥ 85%.
- **Source choice:** the share of plays that use the top-seeded row, and the source-switch rate (shows how good the sort order and badges are).
- **Time to first frame** p50/p90; **rebuffer ratio**.
- **Recommendations:** the share of plays started from Recommended for You and Because you watch *Genre*.
- **Lists:** the share of plays started from My List and Liked.
- **Resume usage:** the share of plays that are resumes (from Continue Watching, History or the Resume row).
- **Retention:** D1/D7/D30 per device.

## 11. Legal, licensing and distribution risks

These need a decision before development goes beyond the prototype:

1. **Copyright exposure.** Searching public torrent sites by title will mostly return copyrighted films and series shared without permission. Streaming them also uploads pieces to other peers. That can create legal liability for users in many countries, and for the developer if the app is seen as built to infringe. Since v0.4, Jazzy **ships with built-in scrapers for named torrent sites, enabled by default**. That makes it much harder to argue that the app is a neutral tool, and it raises the developer's exposure compared with user-configured indexers. What's left as mitigation: no challenge or access-control circumvention (SRC-6), the provider abstraction (SRC-1), which can also host legitimate sources (public-domain archives, Creative Commons catalogues), and a clear in-app disclaimer. **Get legal advice for each target market before distribution.**
2. **Google Play policy.** Play's policies on intellectual property and on apps that facilitate unauthorised streaming make rejection or removal likely for this feature set. Plan for sideloaded or alternative-store distribution, or a Play build with legitimate sources only.
3. **TMDB terms.** TMDB attribution is required (logo plus "This product uses the TMDB API but is not endorsed or certified by TMDB."). Commercial use needs a commercial agreement. Check whether caching TMDB data locally (§6.3) and showing it next to these playback sources is allowed under the terms.
4. **Trademarks.** The layout takes after Netflix's UX patterns, but the app must not use Netflix's name, logo, red "N", "Netflix Original" label, sounds or fonts. "Originals" rails name networks only as factual TMDB metadata.
5. **Open-source licences.** libtorrent (BSD), libtorrent4j (MIT), Media3 (Apache-2.0), FFmpeg extension (LGPL-only configuration, dynamic linking). All are listed on the licences screen.

## 12. Release plan

The schedule, milestones, work breakdown, estimates and release process are in [`DEVELOPMENT_PLAN.md`](DEVELOPMENT_PLAN.md). Summary (baseline, 3 engineers):

| Milestone | Date |
|---|---|
| G1 — Engine go/no-go (after the 3-week engine spike) | 2026-11-06 |
| M1 — Phone alpha (all P0 phone journeys) | 2027-02-26 |
| M2 — Feature complete (P0 + P1, phone and TV) | 2027-03-26 |
| M3 — RC1 / feature freeze | 2027-04-23 |
| Closed beta | 2027-04-26 → 2027-05-21 |
| **GA — v1.0** | **2027-05-25** |

## 13. Open questions

1. **Distribution:** Play Store (legitimate sources only), sideload, or both?
2. **"Multiple streams":** v1 reads this as one torrent pulling pieces from many peers in parallel (TE-4). Should the engine also pull the *same file* from several torrents/info-hashes at once (multi-swarm)? Doing so is much harder and only works when the files are byte-identical.
3. **CAM/TS links:** v0.2 shows all links, with warnings on low-quality ones. Should they be hidden by default instead?
4. **Originals:** which networks are in the default set, and is it one mixed rail or one rail per network?
5. **Profiles across devices:** is local-only fine for v1, given that phone and TV in the same home won't share history or resume points?
6. **Seeding:** what default seeding policy fits the target users' bandwidth and legal context?
