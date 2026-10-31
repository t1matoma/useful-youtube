# Проект: YouTube без Shorts (личное Android-приложение)

## Цель
Android-приложение для личного использования: лента новых видео из моих подписок без Shorts, просмотр видео во встроенном плеере. Без рекомендаций, без сервера — всё работает на телефоне.

## Функциональность MVP
1. Импорт подписок из Google Takeout (`subscriptions.csv`: Channel Id, Channel Url, Channel Title).
2. Получение новых видео каждого канала через RSS: `https://www.youtube.com/feeds/videos.xml?channel_id={id}` (параллельно, с ограничением конкурентности).
3. Получение длительности и деталей через YouTube Data API v3 `videos.list` (part=contentDetails,snippet), батчами по 50 id.
4. Фильтрация Shorts через интерфейс `ShortsDetector` (стратегия):
   - `DurationShortsDetector`: длительность ≤ 180 с;
   - `UrlShortsDetector`: HEAD `https://www.youtube.com/shorts/{id}` без редиректа = Shorts;
   - композиция стратегий; трансляции и премьеры (длительность P0D) не считать Shorts.
5. Лента: все видео подписок, сортировка по дате, pull-to-refresh, состояния загрузки/ошибки/пусто.
6. Экран видео: плеер (`android-youtube-player` от PierfrancescoSoffritti), название, канал, описание.
7. Экран каналов: список подписок, удаление, добавление по ссылке или id.
8. Фоновое обновление через WorkManager (раз в час, только при наличии сети).
9. Офлайн-первый подход: UI всегда читает из Room, сеть только обновляет базу.

## Стек
- Kotlin, Jetpack Compose, Material 3, minSdk 26, последние стабильные версии библиотек
- Gradle Kotlin DSL, version catalog (`libs.versions.toml`), convention plugins в `build-logic`
- Hilt (DI), Coroutines + Flow, Room, Retrofit/OkHttp + kotlinx.serialization, XML-парсер для RSS, Coil, WorkManager, Navigation Compose
- API-ключ берётся из `local.properties` → `BuildConfig`, никогда не коммитится
- Только официальные/публичные источники (Data API, RSS, IFrame-плеер). Никаких неофициальных API

## Архитектура
Clean Architecture + MVVM, однонаправленный поток данных (UDF).
Многомодульная структура:
- `:app` — точка входа, навигация, DI-граф
- `:core:model` — доменные модели (чистый Kotlin)
- `:core:domain` — use cases, интерфейсы репозиториев, `ShortsDetector`
- `:core:data` — реализации репозиториев, мапперы
- `:core:database` — Room (entities, DAO, миграции)
- `:core:network` — Retrofit-сервисы, RSS-парсер, DTO
- `:core:ui` — общие Compose-компоненты, тема
- `:core:testing` — фейки, тестовые данные, правила
- `:feature:feed`, `:feature:player`, `:feature:channels`, `:feature:import`

Правила:
- Зависимости только внутрь: feature → domain ← data. Feature-модули не зависят друг от друга
- Доменный слой без Android-зависимостей
- ViewModel выставляет один `StateFlow<UiState>` (sealed/data class) и принимает события через `onEvent(UiEvent)`
- Composable-экраны stateless: `Screen(state, onEvent)` + отдельный `Route`, подключённый к ViewModel
- Ошибки моделируются явно (`Result`/sealed-типы), без проброса исключений в UI
- Диспетчеры инжектятся, не хардкодятся

## Стиль кода
- ktlint + detekt с конфигами в репозитории; код должен проходить их без предупреждений
- Официальный Kotlin code style, понятные имена, без сокращений
- Без «магических чисел»: константы с названиями
- KDoc только для публичного API, где назначение неочевидно
- Одинаковые паттерны во всех feature-модулях (структура пакетов, нейминг: `FeedViewModel`, `FeedUiState`, `FeedScreen`, `FeedRoute`)

## Тесты
- Юнит-тесты (JUnit, kotlinx-coroutines-test, Turbine, Truth/AssertK): use cases, `ShortsDetector`, мапперы, CSV- и RSS-парсеры, ViewModel
- Фейки вместо моков, где возможно (фейковые репозитории в `:core:testing`)
- Room: тесты DAO на in-memory базе
- Сеть: MockWebServer с JSON/XML-фикстурами
- Compose UI: тесты ключевых экранов (лента, состояния ошибки и пустой ленты)
- WorkManager: тест воркера через `TestListenableWorkerBuilder`
- Цель — покрытие доменного и data-слоёв ≥ 80% (Kover)
- `./gradlew check` должен проходить полностью (тесты + ktlint + detekt)

## Порядок работы
1. Сначала покажи план: структуру модулей, ключевые интерфейсы и модели. Дождись моего подтверждения
2. Работай поэтапно, каждый этап — рабочее состояние с тестами:
   1. Скелет проекта, build-logic, CI-проверки (ktlint, detekt, Kover)
   2. Модели + domain + `ShortsDetector` с тестами
   3. Network: RSS-парсер, Data API клиент
   4. Database + репозитории (офлайн-первый подход)
   5. Импорт подписок
   6. Экран ленты
   7. Плеер
   8. Экран каналов
   9. WorkManager
3. После каждого этапа: запусти `./gradlew check`, исправь ошибки, сделай коммит в формате Conventional Commits, кратко опиши, что сделано
4. Если требование неоднозначно — спроси, а не угадывай
5. Не добавляй функциональность сверх описанной без согласования