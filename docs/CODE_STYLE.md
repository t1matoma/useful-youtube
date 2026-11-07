# Code style

Code must pass `./gradlew check` without warnings. Kotlin compiler warnings are errors
(`allWarningsAsErrors`), and detekt runs with `warningsAsErrors: true`.

## Tools

| Tool | Config | Notes |
|---|---|---|
| ktlint 1.8 | `.editorconfig` | `ktlint_official` style, 120 characters per line; `@Composable` functions may be PascalCase |
| detekt 1.23 | `config/detekt/detekt.yml` | on top of the default config (`buildUponDefaultConfig`) |
| Kover | `KoverConventionPlugin` | ≥ 80% line coverage in `:core:domain` and `:core:data` |
| Android Lint | defaults | runs as part of `check` |

Fix formatting with `./gradlew ktlintFormat` before running `check`.

Deviations from the default detekt rules, and why:

- `FunctionNaming`, `LongParameterList` and `LongMethod` ignore `@Composable`: screens take many
  callback parameters and use PascalCase names.
- `ReturnCount`: at most 3, guard clauses are not counted.
- `MagicNumber` does not check tests and `*.kts` files.

## Naming and structure

- Official Kotlin style, clear names without abbreviations (`channelRepository`, not `chRepo`).
- No magic numbers: use named constants (`MAXIMUM_CONCURRENT_REQUESTS`, `THUMBNAIL_ASPECT_RATIO`).
  Compose dimensions are private values such as `private val ContentPadding = 16.dp`.
- KDoc only for public API whose purpose is not obvious. Comments explain *why*, not *what*.
- Classes used only inside their module (repository implementations, Hilt modules) are `internal`.
- External representations (DTOs, entities) become domain models through extension functions in
  `mapper/` (`toVideo()`, `toEntity()`).

### Feature module layout

Every feature module uses the same packages and names (example: the feed):

```
feature/feed/src/main/kotlin/.../feature/feed/
├── FeedUiState.kt        // sealed interface or data class
├── FeedUiEvent.kt        // sealed interface of events
├── FeedViewModel.kt      // @HiltViewModel, val uiState: StateFlow<FeedUiState>, fun onEvent(FeedUiEvent)
├── FeedScreen.kt         // FeedRoute (wires the ViewModel) + FeedScreen (stateless)
├── component/            // components private to the screen
└── navigation/
    └── FeedDestination.kt  // @Serializable FeedDestination, NavGraphBuilder.feedScreen(), NavController.navigateToX() if needed
```

Rules:

- A ViewModel exposes **one** `StateFlow<XUiState>` and receives events **only** through `onEvent`.
  Internal mutable state has descriptive names (`mutableUiState`, `refreshState`, `editorState`).
- `XScreen(state, onEvent, …navigation callbacks, modifier)` never touches the ViewModel and keeps no
  business state. Only UI state is allowed (`remember { SnackbarHostState() }`, a file picker launcher).
- `XRoute` is the only place with `hiltViewModel()` and `collectAsStateWithLifecycle()`.
- Navigation is done with callbacks (`onVideoClick: (VideoId) -> Unit`), not ViewModel events.
- Navigation files are named `XDestination.kt` (required by detekt's `MatchingDeclarationName`).
- Strings live in `res/values/strings.xml` (English, the default) with a module prefix: `feature_feed_…`,
  `core_ui_…`. Every string also has a Russian translation in `res/values-ru/strings.xml`.

## Errors

- Do not throw into the domain or the UI. Return `AppResult.Success` / `AppResult.Failure` with a typed
  error from `:core:model`.
- Exceptions are caught at the boundaries: network (`NetworkErrorMapper.catching`) and file reading.
  `CancellationException` is always rethrown.
- Every new error type needs a text in `AppError.message()` (`:core:ui`); the exhaustive `when` will
  not compile otherwise.

## Coroutines

- Dispatchers are injected (`@IoDispatcher`), never taken from `Dispatchers.IO` directly.
  The only exception is `DispatchersModule`, which provides them.
- Parallelism is limited with a `Semaphore`; limits are named constants.
- Use `stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initial)` with
  `STOP_TIMEOUT_MILLIS = 5_000L`.

## Tests

- Test names are sentences in backticks: `` fun `failure of one channel is reported but does not stop refresh`() ``.
- Arrange → act → assert, with blocks separated by blank lines.
- Assertions with AssertK (`assertThat(x).isEqualTo(y)`); `Flow`s with Turbine.
- Fakes instead of mocks. Shared fakes live in `:core:testing`; infrastructure fakes stay in the
  module's tests (`FakeYouTubeNetworkDataSource` in `:core:data`).
- For `StateFlow`s with `WhileSubscribed`, subscribe with `collectInBackground(viewModel.uiState)` and
  assert `uiState.value` after `advanceUntilIdle()`. Do not depend on the exact sequence of
  intermediate states produced by `combine`.
- Robolectric tests (Room, Compose, WorkManager) need `src/test/resources/robolectric.properties`
  with `sdk=35`.
- Compose: use `androidx.compose.ui.test.junit4.v2.createComposeRule`; v1 is deprecated and fails the build.
- Network fixtures are realistic YouTube responses in `src/test/resources`.
- Compute expected values (for example epoch milliseconds) instead of writing them from memory.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/): `type(scope): summary` in the imperative
mood, with a bullet list of changes in the body.

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `chore`.
Scopes: a module or feature, such as `domain`, `network`, `data`, `feed`, `player`, `channels`, `import`, `sync`.

Before committing: `./gradlew ktlintFormat check`.
