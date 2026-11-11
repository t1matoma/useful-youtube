# Useful YouTube

An Android app that shows a chronological feed of new videos from your YouTube subscriptions —
**without Shorts and without YouTube's recommendations** — and plays them in the official embedded player.
You can also search all of YouTube explicitly. There is no backend: everything runs on the device.

## Features

- Import all subscriptions from your YouTube account (Google sign-in), or from Google Takeout (`subscriptions.csv`).
- Feed of all subscription videos sorted by date, with pull-to-refresh and refresh on open.
- Playlists on the device: create, rename, save any video from the player; your YouTube playlists,
  read-only and without Shorts. Playlists play in order with previous/next.
- Local watch history grouped by day, with removing single videos and clearing.
- Video search across YouTube, without Shorts: by relevance or by date, recent queries, adding the channel
  of a found video to your subscriptions.
- Shorts filtering: by duration (≤ 180 s), confirmed by checking `youtube.com/shorts/{id}`.
  Live streams and premieres are kept.
- Video screen: player, title, channel, description, chapters from the description with jump on tap.
- Like and dislike, synced with your YouTube account.
- Optional **For you** tab: recommendations computed on the device from your watch history and likes
  (subscriptions plus channels your favorite authors feature); can be turned off in the settings.
- Channel management: add by link, `@handle` or channel id (`UC…`), remove.
- Hourly background refresh when a network is available (WorkManager).
- Offline-first: the UI always shows data from the local database.

Only official data sources are used: channel RSS feeds, YouTube Data API v3 (with an API key, and with
your OAuth authorization for your own subscriptions) and the IFrame player.

> The UI is in English, with a Russian translation that is used when the device language is Russian.

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
- importing from the account costs 1 unit per 50 subscriptions (`subscriptions.list`);
- loading your YouTube playlists costs about 1 unit per 50 playlists plus 2 units per 50 videos;
- a search costs 101 units per page of 50 results (`search.list` + `videos.list`), so the daily limit
  allows about 95 searches;
- RSS feeds cost nothing.

Apart from searches, the default daily limit of 10,000 units is far more than personal use needs.

### 2. Google sign-in (for importing from the account)

Needed only for **Import from YouTube account** and YouTube playlists; the app works without it.

1. In the same Cloud project open **Google Auth Platform** (APIs & Services → OAuth consent screen),
   choose the **External** audience and keep the app in **Testing**. Add your Google account to
   **Test users**.
2. Create an OAuth client: Clients → Create client → **Android**, package name `dev.satoshi.usefulyoutube`,
   and the SHA-1 of the signing certificate. For debug builds, get it with `./gradlew :app:signingReport`
   (variant `debug`).
3. Nothing goes into `local.properties`: Google matches the app by package name and SHA-1.

While the app is in Testing, only test users can sign in and Google may ask to grant access again
after 7 days. The device needs Google Play services.

### 3. Or export your subscriptions with Takeout

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

- [docs/SPECIFICATION.md](docs/SPECIFICATION.md) — current behaviour, constraints and requirements.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — modules, data flow, Shorts detection.
- [docs/CODE_STYLE.md](docs/CODE_STYLE.md) — conventions, screen template, tests, commits.
- [AGENTS.md](AGENTS.md) — guide for AI coding agents.
