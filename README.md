# Useful YouTube

Личное Android-приложение: лента новых видео из ваших подписок YouTube **без Shorts** и без рекомендаций.
Видео открываются во встроенном официальном плеере. Сервер не нужен: всё работает на телефоне.

## Возможности

- Импорт подписок из Google Takeout (`subscriptions.csv`).
- Лента всех видео подписок по дате, pull-to-refresh, обновление при открытии.
- Отсев Shorts: по длительности (≤ 180 с) с проверкой адреса `youtube.com/shorts/{id}`.
  Трансляции и премьеры показываются.
- Экран видео: плеер, название, канал, описание.
- Управление каналами: добавление по ссылке, `@handle` или id (`UC…`), удаление.
- Фоновое обновление раз в час при наличии сети (WorkManager).
- Офлайн-режим: интерфейс всегда показывает данные из локальной базы.

Источники данных только официальные и публичные: RSS-ленты каналов, YouTube Data API v3 и IFrame-плеер.

## Требования

- Android-устройство с Android 8.0 (API 26) или новее.
- Android SDK (platform 37, build-tools 37). Путь указывается в `local.properties` → `sdk.dir`.
- JDK устанавливать не нужно: Gradle сам скачает Adoptium JDK 21 (`gradle/gradle-daemon-jvm.properties`).
- Ключ YouTube Data API v3.
- Около 3 ГБ свободной памяти для сборки. Настройки рассчитаны на машину с 8 ГБ ОЗУ.

## Настройка

### 1. Ключ YouTube Data API

1. В [Google Cloud Console](https://console.cloud.google.com/) создайте проект.
2. Включите **YouTube Data API v3** (APIs & Services → Library).
3. Создайте ключ: Credentials → Create credentials → API key. Желательно ограничить его только YouTube Data API v3.
4. Добавьте в `local.properties` в корне проекта (файл не коммитится):

   ```properties
   sdk.dir=/home/<user>/Android/Sdk
   youtube.api.key=ВАШ_КЛЮЧ
   ```

Квота: обновление ленты расходует 1 единицу на каждые 50 новых видео (`videos.list`),
добавление канала по `@handle` — 1 единицу (`channels.list`). RSS-ленты квоту не расходуют.
Дневного лимита в 10 000 единиц с запасом хватает для личного использования.

### 2. Экспорт подписок

1. Откройте [takeout.google.com](https://takeout.google.com/), снимите все галочки и выберите
   «YouTube и YouTube Music». В настройках оставьте только «подписки».
2. Скачайте и распакуйте архив. Нужен файл `subscriptions.csv` (в русской версии он лежит в папке
   `YouTube и YouTube Music/подписки/`).
3. Скопируйте его на телефон. В приложении: «Импортировать подписки» → выберите файл.

## Сборка и установка

```bash
./gradlew :app:assembleDebug
~/Android/Sdk/platform-tools/adb install app/build/outputs/apk/debug/app-debug.apk
```

Телефон подключается по USB с включённой отладкой.
На машине с 8 ГБ памяти эмулятор не используйте: он вместе со сборкой не поместится в ОЗУ.

## Проверки

```bash
./gradlew check            # тесты, Android Lint, ktlint, detekt, Kover (≥ 80% для domain и data)
./gradlew ktlintFormat     # автоисправление форматирования
./gradlew :core:domain:koverLog :core:data:koverLog   # покрытие
./gradlew --stop           # освободить память после работы
```

Во время разработки удобнее проверять отдельный модуль: `./gradlew :feature:feed:check`.

## Документация

- [ARCHITECTURE.md](ARCHITECTURE.md) — модули, слои, потоки данных, определение Shorts.
- [CODE_STYLE.md](CODE_STYLE.md) — стиль кода, шаблоны экранов, тесты, коммиты.
- [AGENTS.md](AGENTS.md) — руководство для AI-агентов, работающих с репозиторием.
- [CLAUDE.md](CLAUDE.md) — исходная спецификация проекта.
