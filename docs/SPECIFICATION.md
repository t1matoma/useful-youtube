# Specification

What Useful YouTube does and the constraints it must keep. This document describes the current behaviour
of the app. Changing any of it — adding, removing or altering behaviour — requires the maintainer's approval.

## Purpose

An Android app that shows a chronological feed of new videos from the user's YouTube subscriptions,
without Shorts and without recommendations, and plays them in the official embedded player.

## Non-negotiable constraints

- **No backend.** Everything runs on the device.
- **Official sources only:** channel RSS feeds, YouTube Data API v3 (with an API key, or with the user's
  OAuth access token for their own account data), the IFrame player.
  No unofficial APIs, scraping of HTML pages or private endpoints.
- **No recommendations.** The feed shows only videos of channels the user subscribed to. Videos of other
  channels appear only as results of a search the user explicitly started; the app never suggests
  videos on its own (no related videos, trends or autocomplete).
- **Offline-first.** The UI always reads from the local database; the network only updates it.
- **Secrets stay local.** The Data API key comes from `local.properties` → `BuildConfig` and is never committed.
  OAuth needs no client secret or client id in the code: Google identifies the app by its package name and
  signing certificate. Access tokens stay in Google Play services.

## Features

### Look

- The app is always dark, with its own palette (near-black background, coral accent) instead of the
  wallpaper colors, on every Android version. There is no light theme.
- The UI is in English; a Russian translation is used when the device language is Russian.
- Bundled fonts: Inter for text, Onest for headings (both support Cyrillic).
- Own launcher icon (adaptive, with a themed monochrome layer) and a dark splash screen.

### Navigation

A bottom navigation bar switches between **Feed**, **Search**, **Playlists** and **History**; each tab
keeps its state. The video,
playlist, channels and import screens open on top of the tabs without the bar.

### Subscriptions

- Import from the YouTube account: the user authorizes the app with Google (scope
  `https://www.googleapis.com/auth/youtube`, full access, so that the same grant later covers playlists),
  and all subscriptions are loaded with Data API `subscriptions.list?mine=true&part=snippet`, 50 per page,
  following `nextPageToken` (1 quota unit per page). The import runs only when the user asks for it.
  It only adds channels: already subscribed channels, including manually added ones, are kept, and channels
  unsubscribed on YouTube are not removed. A token rejected with HTTP 401 is cleared and replaced once.
  Requires Google Play services.
- Import from Google Takeout `subscriptions.csv` (columns: Channel Id, Channel Url, Channel Title).
  Columns are read by position because Takeout localizes the header. Already subscribed channels are skipped.
- Add a channel by id (`UC…`), by `@handle`, or by a link `youtube.com/channel/UC…` / `youtube.com/@handle`.
  An id is verified through the channel's RSS feed; a handle is resolved with Data API
  `channels.list?forHandle=`.
- Remove a channel after confirmation; its videos are removed with it.

### Feed refresh

1. Load the RSS feed of every channel, `https://www.youtube.com/feeds/videos.xml?channel_id={id}`,
   in parallel with at most 8 concurrent requests.
2. Load durations and details of new videos with Data API `videos.list`
   (`part=contentDetails,snippet`) in batches of 50 ids.
3. Classify new videos as regular, Short, or live stream/premiere (see below).

A failure of a single channel does not stop the refresh and is not shown in the UI. The refresh fails
only when every channel failed or the details request failed.

The refresh runs when the feed opens, on pull-to-refresh, and hourly in the background with WorkManager
when a network connection is available.

### Shorts detection

Implemented as the `ShortsDetector` strategy:

- `DurationShortsDetector`: a video of 180 seconds or less is a Short.
- `UrlShortsDetector`: `HEAD https://www.youtube.com/shorts/{id}` without following redirects; `200` means
  a Short, a redirect to `/watch` means a regular video, anything else is inconclusive.
- `CompositeShortsDetector`: videos longer than 180 seconds are regular without any request; shorter ones
  are decided by the URL check; if the URL check is inconclusive, the duration verdict is kept.
- Live streams and premieres (duration `P0D`) are never Shorts and stay in the feed.
- Videos whose details are not loaded yet are hidden until they are classified.

### Search

- The **Search** tab. Searches all of YouTube with Data API
  `search.list?type=video&part=id&maxResults=50` (100 quota units per page), with `regionCode` and
  `relevanceLanguage` taken from the device locale.
- Details of the found videos are loaded with `videos.list` (1 unit), and Shorts are removed with the same
  detector as in the feed. Live streams and premieres stay. Videos without details are skipped.
- Order: by relevance (default) or by date. Changing the order searches the current query again.
- 50 results per page; the next page is loaded only with the **Show more** button.
- Results and the query of the last search are stored in Room and shown again when the screen opens.
  A new search replaces them; a failed search keeps them.
- Up to 10 recent queries are stored locally and shown while the query field is empty; each one can be
  searched again or deleted. A query is remembered even if its search fails.
- No autocomplete: YouTube has no official API for suggestions.

### Playlists

- App playlists are stored only on the device and cost no quota: create, rename, delete (after
  confirmation), remove videos. A playlist contains each video once, in the order the videos were added.
- Any video can be saved: from the feed, the search or another playlist. The playlist keeps a copy of the
  video's data, so it plays even if the video is not in the feed or the last search anymore.
- The **Playlists** tab lists the playlists by title with their video count and the first video's
  thumbnail, in two sections: **My playlists** and **YouTube**. A playlist screen lists its videos with
  **Play all**; tapping a video plays the playlist from that video on. Pull-to-refresh loads the YouTube playlists again.
- YouTube playlists are a read-only copy of the account's playlists. They are loaded when the tab opens
  (once per screen) and on pull-to-refresh: `playlists.list?mine=true` (1 unit per 50 playlists),
  `playlistItems.list` per playlist (1 unit per 50 videos) and `videos.list` for all their videos
  (1 unit per 50). The account is the source of truth: playlists and videos removed on YouTube disappear.
- Shorts are hidden in YouTube playlists with the feed's detector; videos classified on an earlier sync are
  not checked again. Private and deleted videos have no details and are hidden. The stored copy stays
  available offline and when a sync fails.
- Without a connected account the YouTube section offers **Connect YouTube account**, which shows the
  Google consent screen. "Watch later" is not available: the Data API does not return it.

### Watch history

- Stored only on the device: the Data API does not return the account's history. Costs no quota.
- A video is recorded when its playback starts (the player reports `PLAYING`), not when the screen opens;
  pausing and resuming does not record it again. Every video of a played playlist is recorded.
- Each video appears once; watching it again moves it to the top with the new time. Only the 500 most
  recently watched videos are kept.
- The history keeps a copy of the video's data, so its videos play even if they left the feed, the search
  and the playlists.
- The **History** tab groups videos by the local day they were watched (**Today**, **Yesterday**, then the
  date), newest first. A video can be removed; **Clear history** removes all after confirmation. Tapping a
  video opens it.

### Feed screen

- All regular videos and live streams/premieres of all subscriptions, newest first.
- Each item: an edge-to-edge thumbnail, duration or a live badge, title, channel, relative publication date.
- Pull-to-refresh; loading, empty (with a shortcut to import), and error (with retry) states.
- A refresh error over stored videos is shown once in a snackbar.

### Video screen

- Official IFrame player ([android-youtube-player](https://github.com/PierfrancescoSoffritti/android-youtube-player))
  with `rel=0`, so related videos at the end come from the same channel only.
- Title, channel, relative publication date, and a selectable description that can be expanded and collapsed.
- Playback survives screen rotation.
- **Fullscreen** through the player's own fullscreen button: the video covers the screen in landscape with
  the system bars hidden. The same button or Back leaves fullscreen. In a playlist, **Previous** and
  **Next** are shown on the sides while the video is paused.
- A playlist plays in order: when a video ends, the next one starts. A bar under the actions shows
  "‹playlist› · 3 of 12" with **Previous** and **Next**. Back returns to the playlist.
- **Save to playlist** opens a sheet with the app playlists, ticked when they contain the video, and
  **New playlist**, which creates a playlist with the video.
- For a video of a channel that is not subscribed (opened from the search), an **Add channel to
  subscriptions** button. The channel id is verified through its RSS feed, so it costs no quota.

## Tech stack

- Kotlin, Jetpack Compose, Material 3, minSdk 26, latest stable library versions.
- Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`), convention plugins in `build-logic`.
- Hilt, Coroutines + Flow, Room, Retrofit/OkHttp + kotlinx.serialization, xmlutil for RSS, Coil,
  WorkManager, Navigation Compose.

## Architecture rules

Clean Architecture + MVVM with unidirectional data flow. Details: [ARCHITECTURE.md](ARCHITECTURE.md).

- Modules: `:app`, `:core:model`, `:core:domain`, `:core:data`, `:core:database`, `:core:network`,
  `:core:ui`, `:core:auth`, `:core:testing`, `:feature:feed`, `:feature:player`, `:feature:channels`, `:feature:import`,
  `:feature:search`, `:feature:playlists`, `:feature:history`.
- Dependencies point inwards: feature → domain ← data. Feature modules do not depend on each other.
- `:core:model` and `:core:domain` have no Android dependencies.
- A ViewModel exposes a single `StateFlow<UiState>` and receives events through `onEvent(UiEvent)`.
- Screens are stateless `Screen(state, onEvent)` composables with a separate `Route` connected to the ViewModel.
- Errors are explicit (`AppResult` with typed errors); exceptions never reach the UI.
- Coroutine dispatchers are injected.

## Quality requirements

Details: [CODE_STYLE.md](CODE_STYLE.md).

- ktlint and detekt pass without warnings; Kotlin warnings are errors.
- Official Kotlin code style, clear names without abbreviations, no magic numbers.
- The same structure and naming in every feature module (`FeedViewModel`, `FeedUiState`, `FeedScreen`, `FeedRoute`).
- Tests:
  - unit tests for use cases, Shorts detectors, mappers, parsers and ViewModels;
  - fakes instead of mocks;
  - DAO tests on an in-memory database;
  - MockWebServer with fixtures for the network;
  - Compose UI tests of the feed states;
  - a worker test with `TestListenableWorkerBuilder`.
- Line coverage of `:core:domain` and `:core:data` is at least 80% (Kover).
- `./gradlew check` passes completely.
