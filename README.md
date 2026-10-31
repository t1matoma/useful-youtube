# Useful YouTube

An Android app that shows a chronological feed of new videos from your YouTube subscriptions —
**without Shorts and without recommendations** — and plays them in the official embedded player.
There is no backend: everything runs on the device.

## Features

- Import subscriptions from Google Takeout (`subscriptions.csv`).
- Feed of all subscription videos sorted by date, with pull-to-refresh and refresh on open.
- Shorts filtering: by duration (≤ 180 s), confirmed by checking `youtube.com/shorts/{id}`.
  Live streams and premieres are kept.
- Video screen: player, title, channel, description.
- Channel management: add by link, `@handle` or channel id (`UC…`), remove.
- Hourly background refresh when a network is available (WorkManager).
- Offline-first: the UI always shows data from the local database.

Only official, public data sources are used: channel RSS feeds, YouTube Data API v3 and the IFrame player.

> The UI is currently in Russian.

## Requirements

- An Android device or emulator running Android 8.0 (API 26) or newer.
- Android SDK with platform 37 and build-tools 37. Set `sdk.dir` in `local.properties` or `ANDROID_HOME`.
- No local JDK is needed: Gradle downloads JDK 21 automatically (`gradle/gradle-daemon-jvm.properties`).
- A YouTube Data API v3 key.

## Setup

### 1. YouTube Data API key

1. Create a project in the [Google Cloud Console](https://console.cloud.google.com/).
2. Enable **YouTube Data API v3** (APIs & Services → Library).
3. Create a key: Credentials → Create credentials → API key. Restricting it to YouTube Data API v3 is recommended.
4. Add it to `local.properties` in the project root (this file is git-ignored):

   ```properties
   sdk.dir=/path/to/Android/Sdk
   youtube.api.key=YOUR_KEY
   ```

Quota usage:

- a refresh costs 1 unit per 50 new videos (`videos.list`);
- adding a channel by `@handle` costs 1 unit (`channels.list`);
- RSS feeds cost nothing.

The default daily limit of 10,000 units is far more than personal use needs.

### 2. Export your subscriptions

1. Open [takeout.google.com](https://takeout.google.com/), deselect everything and select
   **YouTube and YouTube Music**. In its options, keep only **subscriptions**.
2. Download and unzip the archive, then find `subscriptions.csv` (in the `subscriptions` folder;
   folder names are localized).
3. Copy the file to your phone and choose **Import subscriptions** in the app.

## Build and install

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Checks

```bash
./gradlew check            # unit tests, Android Lint, ktlint, detekt, Kover (≥ 80% for domain and data)
./gradlew ktlintFormat     # fix formatting
./gradlew :core:domain:koverLog :core:data:koverLog   # coverage
```

Gradle memory settings in `gradle.properties` are conservative, so the project builds on machines with
8 GB of RAM. If you have more memory, override them in `~/.gradle/gradle.properties` for faster builds.

## Documentation

- [docs/SPECIFICATION.md](docs/SPECIFICATION.md) — scope, requirements and approved decisions.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — modules, data flow, Shorts detection.
- [docs/CODE_STYLE.md](docs/CODE_STYLE.md) — conventions, screen template, tests, commits.
- [AGENTS.md](AGENTS.md) — guide for AI coding agents.
