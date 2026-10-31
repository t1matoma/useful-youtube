# Specification

Product and engineering requirements for Useful YouTube. This is the source of truth for scope:
anything not described here needs the maintainer's approval before it is built.

## Goal

An Android app that shows a feed of new videos from your YouTube subscriptions without Shorts and plays
them in an embedded player. No recommendations and no backend: everything runs on the device.

## MVP features

1. Import subscriptions from Google Takeout (`subscriptions.csv`: Channel Id, Channel Url, Channel Title).
2. Load each channel's latest videos from its RSS feed
   `https://www.youtube.com/feeds/videos.xml?channel_id={id}`, in parallel with limited concurrency.
3. Load durations and details with YouTube Data API v3 `videos.list` (`part=contentDetails,snippet`)
   in batches of 50 ids.
4. Filter Shorts through the `ShortsDetector` interface (strategy pattern):
   - `DurationShortsDetector`: duration ≤ 180 s;
   - `UrlShortsDetector`: `HEAD https://www.youtube.com/shorts/{id}` without a redirect means a Short;
   - a composition of strategies. Live streams and premieres (duration `P0D`) are never Shorts.
5. Feed: all subscription videos sorted by date, pull-to-refresh, loading, error and empty states.
6. Video screen: player ([android-youtube-player](https://github.com/PierfrancescoSoffritti/android-youtube-player)),
   title, channel, description.
7. Channels screen: list of subscriptions, removal, adding by link or id.
8. Background refresh with WorkManager (hourly, only when a network is available).
9. Offline-first: the UI always reads from Room; the network only updates the database.

### Approved additions and decisions

- Shorts composition: duration > 180 s → not a Short (no request); ≤ 180 s → the `/shorts/{id}` check
  decides; if the check fails → the duration verdict is kept.
- Videos whose details are not loaded yet are hidden until they are classified.
- Channels can also be added by `@handle` (Data API `channels.list?forHandle=`).
- The feed refreshes when it opens.
- The player uses `rel=0` so that related videos come from the same channel only.
- Removing a channel asks for confirmation.
- The video description can be expanded and collapsed.
- The player survives screen rotation (`configChanges`).
- Partial refresh failures (only some channels failed) are not shown in the UI.
- Quality checks run locally (`./gradlew check`); there is no CI service.

## Tech stack

- Kotlin, Jetpack Compose, Material 3, minSdk 26, latest stable library versions.
- Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`), convention plugins in `build-logic`.
- Hilt, Coroutines + Flow, Room, Retrofit/OkHttp + kotlinx.serialization, an XML parser for RSS (xmlutil),
  Coil, WorkManager, Navigation Compose.
- The API key comes from `local.properties` → `BuildConfig` and is never committed.
- Only official/public sources: Data API, RSS, IFrame player. No unofficial APIs.

## Architecture

Clean Architecture + MVVM with unidirectional data flow. Modules:

- `:app` — entry point, navigation, DI graph;
- `:core:model` — domain models (pure Kotlin);
- `:core:domain` — use cases, repository interfaces, `ShortsDetector`;
- `:core:data` — repository implementations, mappers;
- `:core:database` — Room (entities, DAO, migrations);
- `:core:network` — Retrofit services, RSS parser, DTOs;
- `:core:ui` — shared Compose components, theme;
- `:core:testing` — fakes, test data, rules;
- `:feature:feed`, `:feature:player`, `:feature:channels`, `:feature:import`.

Rules:

- Dependencies point inwards: feature → domain ← data. Feature modules do not depend on each other.
- The domain layer has no Android dependencies.
- A ViewModel exposes a single `StateFlow<UiState>` (sealed or data class) and receives events through
  `onEvent(UiEvent)`.
- Composable screens are stateless: `Screen(state, onEvent)` plus a separate `Route` connected to the ViewModel.
- Errors are modelled explicitly (`AppResult` / sealed types); exceptions never reach the UI.
- Dispatchers are injected, not hardcoded.

Details: [ARCHITECTURE.md](ARCHITECTURE.md).

## Code style

- ktlint and detekt with configs in the repository; code passes them without warnings.
- Official Kotlin code style, clear names without abbreviations.
- No magic numbers: named constants.
- KDoc only for public API whose purpose is not obvious.
- The same patterns in every feature module (package structure, naming such as `FeedViewModel`,
  `FeedUiState`, `FeedScreen`, `FeedRoute`).

Details: [CODE_STYLE.md](CODE_STYLE.md).

## Tests

- Unit tests (JUnit, kotlinx-coroutines-test, Turbine, AssertK): use cases, `ShortsDetector`, mappers,
  CSV and RSS parsers, ViewModels.
- Fakes instead of mocks where possible (fake repositories in `:core:testing`).
- Room: DAO tests on an in-memory database.
- Network: MockWebServer with JSON/XML fixtures.
- Compose UI: tests of key screens (feed, error and empty states).
- WorkManager: worker test with `TestListenableWorkerBuilder`.
- Coverage of the domain and data layers ≥ 80% (Kover).
- `./gradlew check` must pass completely (tests + lint + ktlint + detekt + Kover).

## Implementation history

The MVP was built in these stages, each ending with a green `./gradlew check` and a commit:

1. Project skeleton, build-logic, quality checks (ktlint, detekt, Kover).
2. Models, domain and `ShortsDetector` with tests.
3. Network: RSS parser, Data API client.
4. Database and offline-first repositories.
5. Subscription import.
6. Feed screen.
7. Player.
8. Channels screen.
9. WorkManager.
