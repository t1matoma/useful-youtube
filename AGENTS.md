# AGENTS.md

Guide for AI coding agents (Claude Code, Codex and others) working on this repository.

- Scope and requirements: [docs/SPECIFICATION.md](docs/SPECIFICATION.md). It takes precedence over this file.
- Design: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
- Conventions: [docs/CODE_STYLE.md](docs/CODE_STYLE.md).

## Project in brief

Android app (Kotlin, Jetpack Compose, Material 3, minSdk 26) that shows a feed of YouTube subscription
videos without Shorts. It uses Clean Architecture + MVVM + UDF, a multi-module Gradle build, and is
offline-first on Room. Only official data sources are allowed: RSS, YouTube Data API v3, the IFrame player.

## Working rules

1. **Do not add functionality beyond the specification without the maintainer's approval.**
   If a requirement is ambiguous, ask instead of guessing.
2. For non-trivial work, propose a plan first and wait for confirmation.
3. Work in small steps; every step leaves the project working and tested.
4. Before committing, run `./gradlew ktlintFormat check`. The build must pass without warnings.
5. Use Conventional Commits (see `docs/CODE_STYLE.md`). Commit only when asked or as part of an approved plan.
6. After each step, briefly report what was done and anything that deviated from the plan.
7. Never commit `local.properties`; it contains the SDK path and the API key.
8. Keep the dependency rules: feature → domain ← data, no feature-to-feature dependencies, no Android
   in `:core:model` and `:core:domain`.

## Commands

```bash
./gradlew ktlintFormat                    # format
./gradlew :core:data:check                # check one module (faster while iterating)
./gradlew check                           # everything: tests, lint, ktlint, detekt, Kover
./gradlew :core:domain:koverLog :core:data:koverLog
./gradlew :app:assembleDebug              # app/build/outputs/apk/debug/app-debug.apk
./gradlew --stop                          # free memory when done
```

## Environment

- Android SDK with platform 37 and build-tools 37 (`sdk.dir` in `local.properties` or `ANDROID_HOME`).
- No system JDK is required: the Gradle daemon JVM (JDK 21) is provisioned automatically through
  `gradle/gradle-daemon-jvm.properties` and the foojay resolver.
- `gradle.properties` is tuned to build on machines with 8 GB of RAM: 2 GB heap, no parallel execution,
  two workers, Kotlin compiled in-process, 1 GB test JVMs. Do not raise these values in the repository;
  developers with more memory can override them in `~/.gradle/gradle.properties`.
- Prefer checking a single module while iterating and the full `check` before committing.

## Where things live

| Task | Location |
|---|---|
| Add a dependency | `gradle/libs.versions.toml` (latest stable versions) |
| Shared module configuration | `build-logic/convention/.../buildlogic/*ConventionPlugin.kt` |
| Business logic, use cases | `:core:domain` (pure Kotlin) |
| Feed refresh pipeline | `RefreshFeedUseCase` |
| Shorts rules | `core/domain/.../shorts/`; composition in `DataModule.provideShortsDetector` |
| Repositories, mappers, worker | `:core:data` |
| Database schema | `:core:database`; on changes bump the version, add a migration, commit `schemas/` |
| HTTP, DTOs, RSS | `:core:network`; test fixtures in `src/test/resources` |
| User-facing error texts | `core/ui/.../error/ErrorMessages.kt` and `core/ui/src/main/res/values/strings.xml` |
| Test fakes | `:core:testing` |
| Navigation between screens | `app/.../navigation/UsefulYouTubeNavHost.kt` |

## Known pitfalls

- `nl.adaptivity.xmlutil.XmlException` extends `IOException`. `NetworkErrorMapper` must check it before
  `IOException`, otherwise parsing errors are reported as "no network".
- With xmlutil 1.0 use `XML.v1 { policy { … } }`; `defaultPolicy` is deprecated and breaks the build.
- detekt 1.23 is built against Kotlin 2.0. `QualityConventionPlugin` pins the Kotlin version of the
  `detekt` configuration (`detektKotlin` in the catalog); keep it.
- `failOnNoDiscoveredTests = false` is intentional: Hilt and Room generate test sources even in modules
  without tests.
- Use `createComposeRule` from `junit4.v2`; `advanceUntilIdle` needs
  `@OptIn(ExperimentalCoroutinesApi::class)`.
- Robolectric tests need `src/test/resources/robolectric.properties` with `sdk=35`.
- `:core:data` tests that create the database need `testImplementation(libs.androidx.room.runtime)`.
- `PlayerViewModel` reads its argument from `SavedStateHandle` by `PlayerDestination.VIDEO_ID_ARGUMENT`,
  which must match the `videoId` property name.
- The default WorkManager initializer is removed from the manifest; WorkManager is configured by
  `UsefulYouTubeApplication` with `HiltWorkerFactory`.
- The newest Android cmdline-tools redirect `sdkmanager` to the new Android CLI and may hang while
  downloading it. Version 19.0 of cmdline-tools works; run `sdkmanager` with stdin closed (`</dev/null`).
