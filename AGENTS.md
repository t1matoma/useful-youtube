# AGENTS.md

Руководство для AI-агентов (Claude Code, Codex и др.), работающих с этим репозиторием.
Спецификация продукта — в [CLAUDE.md](CLAUDE.md); при расхождении она главнее.
Подробности: [ARCHITECTURE.md](ARCHITECTURE.md), [CODE_STYLE.md](CODE_STYLE.md).

## Кратко о проекте

Android-приложение (Kotlin, Compose, Material 3, minSdk 26): лента видео из подписок YouTube без
Shorts. Clean Architecture + MVVM + UDF, многомодульная сборка, offline-first на Room.
Источники данных только официальные: RSS, YouTube Data API v3, IFrame-плеер.

## Правила работы

1. **Не добавляйте функциональность сверх описанной без согласования с владельцем.**
   При неоднозначном требовании задайте вопрос, а не угадывайте.
2. Изменения делайте небольшими шагами. Каждый шаг должен оставлять проект в рабочем
   состоянии с тестами.
3. Перед коммитом: `./gradlew ktlintFormat check`. Сборка должна проходить без предупреждений.
4. Коммиты в формате Conventional Commits (см. CODE_STYLE.md). Коммитьте только по просьбе
   владельца или в рамках согласованного плана.
5. После этапа кратко опишите владельцу, что сделано и что отклонилось от плана.
6. Никогда не коммитьте `local.properties`: там путь к SDK и ключ API.

## Ограничения машины разработчика

Ноутбук с **8 ГБ ОЗУ**. Тяжёлая сборка однажды его уже уронила.

- Не меняйте в сторону увеличения настройки `gradle.properties`: `-Xmx2g`, `org.gradle.parallel=false`,
  `workers.max=2`, Kotlin in-process, тестовые JVM по 1 ГБ.
- Во время работы проверяйте отдельный модуль (`./gradlew :feature:feed:check`), полный `check` —
  перед коммитом.
- Закончив работу, останавливайте демон: `./gradlew --stop`.
- Не запускайте эмулятор.

## Окружение

- Android SDK: `~/Android/Sdk` (platform 37, build-tools 37).
- В системе есть только JRE. JDK 21 для демона Gradle скачивается автоматически
  (`gradle/gradle-daemon-jvm.properties`, foojay resolver). Системную Java не используйте.
- Новейшие cmdline-tools перенаправляют `sdkmanager` на новый Android CLI и зависают.
  Используйте cmdline-tools 19.0 и запускайте `sdkmanager` с `</dev/null`.

## Команды

```bash
./gradlew ktlintFormat                    # форматирование
./gradlew :core:data:check                # проверка одного модуля
./gradlew check                           # всё: тесты, lint, ktlint, detekt, Kover
./gradlew :core:domain:koverLog :core:data:koverLog
./gradlew :app:assembleDebug              # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew --stop
```

## Где что лежит

| Задача | Место |
|---|---|
| Новая зависимость | `gradle/libs.versions.toml` (последние стабильные версии) |
| Общая настройка модулей | `build-logic/convention/.../buildlogic/*ConventionPlugin.kt` |
| Бизнес-логика, use case | `:core:domain` (чистый Kotlin, без Android) |
| Обновление ленты | `RefreshFeedUseCase` |
| Правила Shorts | `core/domain/.../shorts/`, композиция — `DataModule.provideShortsDetector` |
| Репозитории, мапперы, воркер | `:core:data` |
| Схема БД | `:core:database`; меняя её, поднимайте версию, добавляйте миграцию, коммитьте `schemas/` |
| HTTP, DTO, RSS | `:core:network`, фикстуры тестов в `src/test/resources` |
| Тексты ошибок | `core/ui/.../error/ErrorMessages.kt` + `core/ui/src/main/res/values/strings.xml` |
| Фейки для тестов | `:core:testing` |
| Навигация между экранами | `app/.../navigation/UsefulYouTubeNavHost.kt` |

## Согласованные решения (не пересматривать без владельца)

- Shorts: длительность > 180 с → не Shorts; ≤ 180 с → решает HEAD `/shorts/{id}`; при неудаче
  проверки — вердикт по длительности. P0D (трансляция, премьера) — не Shorts.
- Видео без загруженных деталей скрыты, пока их вид не определён.
- Добавление канала: id `UC…`, ссылки `/channel/UC…` и `@handle` (через `channels.list?forHandle=`).
- Проверки только локальные (`./gradlew check`), без CI-сервиса.
- Пакет и applicationId: `dev.satoshi.usefulyoutube`.
- Утверждения в тестах через AssertK, RSS через xmlutil, Compose UI-тесты через Robolectric.
- Одобренные дополнения: обновление ленты при открытии, плеер с `rel=0`, подтверждение
  удаления канала, сворачиваемое описание, плеер сохраняется при повороте (`configChanges`).
- Частичные сбои обновления (часть каналов) в UI не показываются.

## Известные подводные камни

- `nl.adaptivity.xmlutil.XmlException` наследуется от `IOException`. В `NetworkErrorMapper` её нужно
  проверять раньше `IOException`, иначе ошибка разбора станет «нет сети».
- В xmlutil 1.0 используйте `XML.v1 { policy { … } }`: `defaultPolicy` устарел и ломает сборку.
- detekt 1.23 собран под Kotlin 2.0: в `QualityConventionPlugin` его конфигурации принудительно
  выставлена версия `detektKotlin`. Не удаляйте это.
- `failOnNoDiscoveredTests = false`: Hilt и Room генерируют тестовые исходники и в модулях без тестов.
- `createComposeRule` берите из пакета `junit4.v2`, а `advanceUntilIdle` требует
  `@OptIn(ExperimentalCoroutinesApi::class)`.
- Тестам `:core:data`, создающим базу, нужен `testImplementation(libs.androidx.room.runtime)`.
- В `PlayerViewModel` аргумент читается из `SavedStateHandle` по имени
  `PlayerDestination.VIDEO_ID_ARGUMENT`. Оно должно совпадать с именем свойства `videoId`.
- Стандартный инициализатор WorkManager удалён из манифеста. Не вызывайте `WorkManager.getInstance`
  до `Application.onCreate`.
- Ожидаемые значения в тестах (например, epoch-миллисекунды) вычисляйте, а не пишите по памяти.
