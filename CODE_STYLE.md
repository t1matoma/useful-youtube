# Стиль кода

Код должен проходить `./gradlew check` без предупреждений. Предупреждения компилятора Kotlin считаются
ошибками (`allWarningsAsErrors`), detekt работает с `warningsAsErrors: true`.

## Инструменты

| Инструмент | Конфиг | Примечание |
|---|---|---|
| ktlint 1.8 | `.editorconfig` | стиль `ktlint_official`, строка до 120 символов; `@Composable`-функции могут начинаться с заглавной буквы |
| detekt 1.23 | `config/detekt/detekt.yml` | поверх стандартного конфига (`buildUponDefaultConfig`) |
| Kover | `KoverConventionPlugin` | ≥ 80% строк в `:core:domain` и `:core:data` |
| Android Lint | по умолчанию | запускается в `check` |

Форматирование исправляется командой `./gradlew ktlintFormat`, её стоит запускать до `check`.

Отступления от стандартного detekt (и почему):

- `FunctionNaming`, `LongParameterList`, `LongMethod` игнорируют `@Composable`: у экранов много
  параметров-колбэков, и имена у них в PascalCase.
- `ReturnCount`: не больше 3, ранние выходы (guard clauses) не считаются.
- `MagicNumber` не проверяет тесты и `*.kts`.

## Именование и структура

- Официальный стиль Kotlin, понятные имена без сокращений: `channelRepository`, а не `chRepo`.
- Никаких «магических чисел»: значения выносятся в именованные константы
  (`MAXIMUM_CONCURRENT_REQUESTS`, `THUMBNAIL_ASPECT_RATIO`). Размеры в Compose — приватные `val … = 16.dp`.
- KDoc только для публичного API, назначение которого неочевидно. Комментарии объясняют «почему», а не «что».
- Классы, видимые только внутри модуля (реализации репозиториев, Hilt-модули), объявляются `internal`.
- Внешние значения (DTO, entities) конвертируются в доменные модели функциями-расширениями в `mapper/`
  (`toVideo()`, `toEntity()`).

### Структура feature-модуля

Во всех feature-модулях одинаковые пакеты и имена (пример — лента):

```
feature/feed/src/main/kotlin/.../feature/feed/
├── FeedUiState.kt        // sealed interface или data class
├── FeedUiEvent.kt        // sealed interface событий
├── FeedViewModel.kt      // @HiltViewModel, val uiState: StateFlow<FeedUiState>, fun onEvent(FeedUiEvent)
├── FeedScreen.kt         // FeedRoute (подключение к ViewModel) + FeedScreen (stateless)
├── component/            // приватные для экрана компоненты
└── navigation/
    └── FeedDestination.kt  // @Serializable FeedDestination, NavGraphBuilder.feedScreen(), NavController.navigateToX() — если на экран переходят
```

Правила:

- ViewModel выставляет **один** `StateFlow<XUiState>` и принимает события **только** через `onEvent`.
  Внутреннее изменяемое состояние называется `mutableUiState`, `refreshState`, `editorState`.
- `XScreen(state, onEvent, …колбэки навигации, modifier)` не обращается к ViewModel и не хранит
  бизнес-состояния. Допускается только UI-состояние (`remember { SnackbarHostState() }`, лаунчер выбора файла).
- `XRoute` — единственное место с `hiltViewModel()` и `collectAsStateWithLifecycle()`.
- Навигация — колбэки (`onVideoClick: (VideoId) -> Unit`), а не события ViewModel.
- Файл навигации называется `XDestination.kt`, этого требует правило detekt `MatchingDeclarationName`.
- Строки хранятся в `res/values/strings.xml` с префиксом модуля: `feature_feed_…`, `core_ui_…`.
  Язык интерфейса русский.

## Ошибки

- Не бросайте исключения в домен и UI. Возвращайте `AppResult.Success` / `AppResult.Failure` с
  типизированной ошибкой из `:core:model`.
- Исключения ловятся на границе сети (`NetworkErrorMapper.catching`) и при чтении файлов.
  `CancellationException` всегда пробрасывается.
- Новый тип ошибки требует текста в `AppError.message()` (`:core:ui`), иначе `when` не скомпилируется.

## Корутины

- Диспетчеры инжектятся (`@IoDispatcher`), а не берутся из `Dispatchers.IO` напрямую.
  Единственное исключение — `DispatchersModule`.
- Параллельность ограничивается `Semaphore`, лимиты оформляются именованными константами.
- `stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), initial)`, где
  `STOP_TIMEOUT_MILLIS = 5_000L`.

## Тесты

- Имена тестов — предложения в обратных кавычках: `` fun `failure of one channel is reported but does not stop refresh`() ``.
- Структура «подготовка → действие → проверка», блоки разделяются пустой строкой.
- Утверждения через AssertK (`assertThat(x).isEqualTo(y)`); для `Flow` — Turbine.
- Фейки вместо моков. Общие фейки лежат в `:core:testing`, фейки инфраструктуры — в тестах модуля
  (`FakeYouTubeNetworkDataSource` в `:core:data`).
- Для `StateFlow` с `WhileSubscribed` подписывайтесь через `collectInBackground(viewModel.uiState)`
  и проверяйте `uiState.value` после `advanceUntilIdle()`. Не завязывайтесь на точную
  последовательность промежуточных состояний `combine`.
- Robolectric-тесты (Room, Compose, WorkManager) требуют `src/test/resources/robolectric.properties`
  с `sdk=35`.
- Compose: `androidx.compose.ui.test.junit4.v2.createComposeRule`, потому что v1 устарел и сборка
  упадёт на предупреждении.
- Сетевые фикстуры — реальные по форме ответы YouTube в `src/test/resources`.

## Коммиты

[Conventional Commits](https://www.conventionalcommits.org/): `type(scope): описание` на английском,
в повелительном наклонении, тело — список изменений.

Типы: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `chore`.
Scope — модуль или фича: `domain`, `network`, `data`, `feed`, `player`, `channels`, `import`, `sync`.

Перед коммитом: `./gradlew ktlintFormat check`.
