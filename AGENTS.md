# AGENTS.md

Instructions for AI coding agents (Claude Code, Codex and others) maintaining this repository.
Read this file at the start of every session and follow it exactly.

## 1. What you must know every session

- **Project:** Useful YouTube is an Android app (Kotlin, Jetpack Compose, Material 3, minSdk 26) that shows
  a feed of YouTube subscription videos without Shorts and plays them in the official embedded player.
- **Status:** the app is feature-complete. The work now is maintenance: bug fixes, dependency updates,
  refactoring and small approved changes.
- **Scope:** [docs/SPECIFICATION.md](docs/SPECIFICATION.md) describes the current behaviour. It is the
  source of truth. Anything not described there is out of scope until the maintainer approves it.
- **Design:** [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). **Conventions:** [docs/CODE_STYLE.md](docs/CODE_STYLE.md).
  Read the relevant section before changing a layer or a screen.
- **Hard constraints** (never break them):
  - no backend;
  - only official sources (RSS, YouTube Data API v3, IFrame player), no unofficial APIs or scraping;
  - no recommendations;
  - offline-first: the UI reads only from Room;
  - never commit `local.properties` or any API key.
- **Architecture invariants:**
  - feature → domain ← data, no feature-to-feature dependencies;
  - no Android in `:core:model` and `:core:domain`;
  - one `StateFlow<UiState>` plus `onEvent(UiEvent)` per ViewModel;
  - stateless `Screen(state, onEvent)` plus a `Route`;
  - typed `AppResult` errors;
  - injected dispatchers.
- **Definition of green:** `./gradlew check` passes: unit tests, Android Lint, ktlint, detekt with
  warnings as errors, Kotlin warnings as errors, Kover ≥ 80% for `:core:domain` and `:core:data`.

## 2. How to handle a task

Follow these steps in order.

1. **Understand.** Restate the task in one or two sentences. Read the code involved and the matching docs
   section. Reproduce bugs with a failing test before fixing them when possible.
2. **Check scope.** If the task adds, removes or changes user-visible behaviour, or the requirement is
   ambiguous, **ask the maintainer before writing code**. Do not guess, and do not add "while I'm here"
   features.
3. **Plan.** For anything beyond a small, local change (new module, schema change, new dependency, change
   across layers), present a short plan with the affected modules and wait for confirmation.
4. **Implement in small steps.** Each step compiles and is covered by tests. Follow the existing patterns
   and the recipes in section 4.
5. **Verify.**
   - While iterating, check the affected module: `./gradlew :core:data:check`.
   - Before finishing, run `./gradlew ktlintFormat check`.
   - Fix the cause of failures. Never weaken checks to get a green build: no `@Suppress`, no detekt
     config relaxations, no lower coverage threshold, no skipped tests, unless the maintainer agrees.
6. **Update docs** in the same change when behaviour, architecture, conventions, commands or pitfalls
   change (`docs/SPECIFICATION.md`, `docs/ARCHITECTURE.md`, `docs/CODE_STYLE.md`, this file).
7. **Commit** only when the maintainer asked for it or approved a plan that includes it. Use Conventional
   Commits (`type(scope): summary`, see `docs/CODE_STYLE.md`).
8. **Report:**
   - what changed;
   - how it was verified (commands and results);
   - anything that deviated from the plan or that you could not verify (for example behaviour that
     needs a real device or a real API key).

   State failures plainly.

## 3. Commands

```bash
./gradlew ktlintFormat                         # fix formatting
./gradlew :feature:feed:check                  # check one module (fast)
./gradlew check                                # full check before finishing
./gradlew :core:domain:koverLog :core:data:koverLog   # coverage numbers
./gradlew :app:assembleDebug                   # app/build/outputs/apk/debug/app-debug.apk
./gradlew --stop                               # free memory at the end of the session
```

Environment:

- Android SDK with platform 37 and build-tools 37 (`sdk.dir` in `local.properties` or `ANDROID_HOME`).
  The Gradle daemon JDK 21 is provisioned automatically; no system JDK is needed.
- `gradle.properties` is deliberately conservative (2 GB heap, no parallel execution, two workers,
  in-process Kotlin, 1 GB test JVMs) so the build fits in 8 GB of RAM.
  **Do not raise these values in the repository.** Prefer module-level checks while iterating.
- The `youtube.api.key` property in `local.properties` is needed only to run the app, not for tests.
- Importing from the YouTube account also needs an Android OAuth client in Google Cloud (package name +
  debug SHA-1, see README). It can be verified only on a device or emulator with Google Play.

## 4. Recipes

**Add or update a dependency**

1. Edit `gradle/libs.versions.toml` only; use the latest stable version.
2. Reference it as `libs.…` from a module or a convention plugin.
3. Run the full `check`. Kotlin deprecation warnings will fail the build, so migrate deprecated APIs.

**Change Shorts detection**

1. The rules live in `core/domain/.../shorts/`; the composition in `DataModule.provideShortsDetector`.
2. Update the detector tests and `docs/SPECIFICATION.md`.
3. This is a behaviour change, so get approval first.

**Add an error case**

1. Add it to the sealed type in `core/model/.../AppError.kt`.
2. Handle it in `AppError.message()` (`core/ui/.../error/ErrorMessages.kt`) with a new string, in
   `NetworkErrorMapper` if it comes from the network, and in `RefreshFeedWorker.isTemporary` for a `DataError`.
3. Exhaustive `when`s show every place that needs it.

**Change the database schema**

1. Change the entities and DAOs in `:core:database`.
2. Bump `UsefulYouTubeDatabase` `version` and add a `Migration` (or `@AutoMigration`).
3. Commit the new file in `core/database/schemas/`.
4. Add a migration test with `MigrationTestHelper` (`room-testing` is already a test dependency).
5. Never use destructive migration: users would lose their subscriptions.

**Add a screen**

1. Create `feature/<name>` with the `usefulyoutube.android.feature` plugin and a namespace
   `dev.satoshi.usefulyoutube.feature.<name>`.
2. Include it in `settings.gradle.kts` and add it to `app/build.gradle.kts`.
3. Follow the feature template in `docs/CODE_STYLE.md`: `XUiState`, `XUiEvent`, `XViewModel`, `XScreen`
   + `XRoute`, `navigation/XDestination.kt`.
4. Register it in `UsefulYouTubeNavHost`; a tab of the bottom bar also needs a `TopLevelDestination` entry.
5. Add ViewModel tests and, for key states, Compose UI tests.

**Add user-facing text**

- Add it to the module's `res/values/strings.xml` with the module prefix (`feature_feed_…`, `core_ui_…`).
  Never hardcode text in composables.

**Add a use case**

1. Put it in `core/domain/.../usecase/` with `@Inject constructor` and `operator fun invoke`.
2. Test it with the fakes from `:core:testing`; extend the fakes when the repository interfaces change.

## 5. Where things live

| What | Where |
|---|---|
| Versions and libraries | `gradle/libs.versions.toml` |
| Shared Gradle configuration | `build-logic/convention/src/main/kotlin/.../buildlogic/` |
| Lint configs | `.editorconfig` (ktlint), `config/detekt/detekt.yml` |
| Models, `AppResult`, errors | `:core:model` |
| Use cases, repository interfaces, Shorts rules, channel input parser | `:core:domain` |
| Feed refresh pipeline | `RefreshFeedUseCase` |
| Repositories, mappers, Takeout CSV, worker and scheduler, Hilt bindings | `:core:data` |
| Room entities, DAOs, schema | `:core:database` |
| Retrofit API, RSS parser, DTOs, error mapping | `:core:network` (fixtures in `src/test/resources`) |
| Theme, shared components, error texts | `:core:ui` |
| Google account authorization (`AccountAuthorizer`, `AccountConsent`) | `:core:auth` |
| Fakes, test data, `MainDispatcherRule`, `collectInBackground` | `:core:testing` |
| Screens | `:feature:feed`, `:feature:player`, `:feature:channels`, `:feature:import` (package `feature.subscriptionimport`), `:feature:search`, `:feature:playlists`, `:feature:history` |
| Search pipeline | `SearchVideosUseCase`, `LoadMoreSearchResultsUseCase`, `OfflineFirstSearchRepository` |
| Shared video card, thumbnail and duration formatting | `:core:ui` (`core.ui.video`) |
| Text input dialog | `:core:ui` (`TextInputDialog`) |
| Navigation graph, `Application`, manifest | `:app` |

## 6. Known pitfalls

- `nl.adaptivity.xmlutil.XmlException` extends `IOException`. `NetworkErrorMapper` must check it first,
  otherwise parsing errors are reported as "no network".
- xmlutil 1.0: use `XML.v1 { policy { … } }`; `defaultPolicy` is deprecated.
- detekt 1.23 is built against Kotlin 2.0. `QualityConventionPlugin` pins the Kotlin version of the
  `detekt` configuration (`detektKotlin` in the catalog). Keep it until detekt 2 is stable.
- `failOnNoDiscoveredTests = false` is intentional: Hilt and Room generate test sources in modules without tests.
- Compose tests use `androidx.compose.ui.test.junit4.v2.createComposeRule`; `advanceUntilIdle` needs
  `@OptIn(ExperimentalCoroutinesApi::class)`.
- Do not assert exact sequences of intermediate states from `combine`. Use
  `collectInBackground(viewModel.uiState)` and assert `uiState.value` after `advanceUntilIdle()`.
- Robolectric tests need `src/test/resources/robolectric.properties` with `sdk=35`.
- `:core:data` tests that build the database need `testImplementation(libs.androidx.room.runtime)`.
- `PlayerViewModel` reads `PlayerDestination.VIDEO_ID_ARGUMENT` from `SavedStateHandle`; it must match the
  `videoId` property name.
- The default WorkManager initializer is removed from the manifest; `UsefulYouTubeApplication` provides
  the configuration with `HiltWorkerFactory`.
- `AuthorizationClient` access tokens expire. Run Data API calls with a token through
  `AccountAuthorizer.withAccessToken`, which clears a rejected token and retries once.
- `IntentSender` cannot be created in plain JVM tests; ViewModel tests that need one run on Robolectric.
- The `import` keyword cannot be a package segment, so `:feature:import` uses `feature.subscriptionimport`.
- `MigrationTestHelper` reads the schemas from the test assets; `:core:database` adds `schemas/` to them
  and makes the unit test assets depend on `copyRoomSchemas`, so a new `N.json` is available in the same
  run. Commit the new `N.json`.
- detekt's `TooManyFunctions` (10 per class or interface) also counts DAO, repository and fake methods;
  split by responsibility instead of suppressing it (`SearchDao`/`RecentSearchQueryDao`,
  `PlaylistRepository`/`MirroredPlaylistRepository`).
- `search.list` costs 100 quota units per page. Never load search pages automatically.
- Create ViewModels inside the test or `by lazy`, not as a field initializer: `viewModelScope` captures
  `Dispatchers.Main.immediate` when the ViewModel is created, before `MainDispatcherRule` replaces it.
- Do not trust `yt:channelId` of RSS feeds: the feed header carries it without the `UC` prefix and it may
  differ from the subscription. Videos take the id of the channel whose feed was requested; a mismatch
  used to fail the `videos.channel_id` foreign key and crash the app on refresh.
- Compute expected values in tests (for example epoch milliseconds) instead of writing them from memory.
- The newest Android cmdline-tools may hang while redirecting `sdkmanager` to the new Android CLI.
  cmdline-tools 19.0 works; run `sdkmanager` with stdin closed (`</dev/null`).
