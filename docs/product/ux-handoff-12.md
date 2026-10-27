# Задача 12 — финализация и передача разработчику

Дата: 27 октября 2026. База: `main / 324ba2f`, после `git pull --ff-only`.
Ветка: `ux-handoff`. Объём: Figma и документация, без продуктового кода.

**Handoff подготовлен; сквозная приёмка дизайна остаётся частичной.**
V-04–V-06 Blocked, V-07 Open, V-09 Blocked for runtime. Передача не снимает эти статусы,
не означает пользовательского одобрения рабочего направления или реализации дизайна.

## Как взять задачу

1. Выбрать H12-xx в backlog ниже и проверить зависимости и gate.
2. Открыть указанную строку карты экранов: там состояния, компоненты и текущий модуль.
3. Прочитать контракт поведения и связанный handoff 06–10; открыть stress-состояния из 11.
4. Включить строки из [реестра RU/EN/KK](ux-handoff-12-copy.md) и проверки из карточки задачи.
5. Реализовывать отдельным PR. Не переносить демонстрационные значения и таймеры Figma в продукт.

## Финальные входы

[13_Handoff — входная страница](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=163-1396).
[Presentation: навигатор](https://www.figma.com/proto/kElntn3nRWIQuFjyxUJNBM?node-id=163-1396&starting-point-node-id=163%3A1396).
Он содержит ссылки на исходные frames, спецификацию, строки и отчёт проверки; копии экранов не создавались.

| Сценарий | Старт | Завершение и возврат | Доказательство / граница |
| --- | --- | --- | --- |
| Начать дневное занятие | [H01](https://www.figma.com/proto/kElntn3nRWIQuFjyxUJNBM?node-id=47-310&starting-point-node-id=47%3A310) | Ready P08 → попытка → повтор / следующий пункт → H03; выход к исходному Сегодня | Ошибка/повтор graph checked. Успешный непрерывный путь V-04 и продолжение Wide Eye V-05 Blocked; H03 — самостоятельный fixture |
| Выбрать свободную игру | [C01](https://www.figma.com/proto/kElntn3nRWIQuFjyxUJNBM?node-id=56-18&starting-point-node-id=56%3A18) | C02 → Classic/Binary/Custom → Ready → итог → повтор через Ready / режимы | Number Sprint graph checked; Custom сохраняет точную серию. Остальные игровые границы не полные игровые прототипы |
| Осмыслить прогресс | [G04](https://www.figma.com/proto/kElntn3nRWIQuFjyxUJNBM?node-id=94-167&starting-point-node-id=94%3A167) | Серия / день → детали → исходный обзор; профиль и настройки вложены | Независимые fixtures graph checked; запись предыдущей игры и сохранение выбранного контекста не доказаны |

Возвращающийся пользователь: H02 `47:311` и H04 `47:313` — отдельные starts/fixtures.
Новичок в Прогрессе: G01 `94:39`; пустой период/legacy: G02 `94:98`; одна попытка: G03 `94:140`.
Межстраничные URL-мосты открывают отдельные вкладки и **не задают runtime back stack**.

## Единственный рабочий вариант и Archive

Приоритет при противоречиях: доработки игры в [11](ux-validation-11.md), затем актуальные
[08](ux-play-result-08.md), [09](ux-progress-09.md), [10](ux-profile-settings-10.md),
[06 v2](ux-home-training-06.md) / [07 v2](ux-catalog-07.md); 05 — базовые состояния и токены.
Архитектура [02](ux-architecture-02.md) сохраняется: Сегодня / Игры / Прогресс,
профиль внутри Прогресса, настройки доступны из трёх корней.

- Рабочий визуальный язык: Noto Sans, игровые иллюстрации и скруглённые поверхности 06/07 v2,
  `catalog/bg`, радиусы карточек 20 / контролов 12; semantic action/error/focus из Foundations.
- Рабочая корневая панель — `Navigation/Bottom` (`81:508`), три равные горизонтальные вкладки,
  иконка + постоянная подпись + индикатор. При 150% панель остаётся горизонтальной.
- Засечки, плоские карточки и текстовая/stacked навигация из 04/05 не являются текущим UI.
  Старые спецификации сохранены как история; указания v2 имеют приоритет.
- Страница `04_Direction` переименована в `Archive / 04_Direction + Catalog v1` (`27:2`).
  Туда перенесён отвергнутый каталог v1 (`69:666`). IDs и редактируемость сохранены.
- Устаревшие masters отмечены `Archive/`: NavigationItem `36:35`, Navigation `37:500`,
  GameItem `55:151`, ModeItem `55:167`, Catalog/Navigation `59:150`. Они сохранены для старых instances.
- 03_Wireframes остаётся доступной: на неё ещё ссылается V-05. Это граница незавершённого сценария,
  а не конкурирующая утверждённая визуальная версия. После закрытия H12-01 её можно архивировать.
- `WF03 / Grayscale`, `archive/A/*` и старые стили не удаляются: они нужны историческим материалам.
  `OquTurbo / Foundations` — рабочая коллекция, Light/Dark. Новая палитра не добавлена.
- Имена H/C/P/G/V и конфигурации сохранены. Безымянные Progress Flow 1–3 заменены именами сценариев;
  добавлен start G04. Profile Flow 1–2 также получили явные названия. Префикс P в 08 и 10 локален странице: в задачах всегда указывается page/node ID.

Native Figma Specs недоступны: `whoami` вернул `specs_enabled: false` для доступного плана.
Спецификация хранится в этом репозитории; `13_Handoff` — навигатор, не имитация native spec
текстовыми слоями. Code Connect не создавался: будущие компоненты ещё не реализованы.

## Экран → состояния → компоненты → текущий модуль

Ссылки в столбце «Источник» содержат полные таблицы frames и stress-вариантов. ID компонентов
разрешаются в том же Figma-файле. Все изменения будущие; имена Figma не объявлены Kotlin API.

| Экран / состояния | Компоненты Figma | Текущий модуль и точки изменения | Источник / задача |
| --- | --- | --- | --- |
| Корневой shell: 3 выбранные вкладки, pressed/focus, 320/150%, wide | Navigation/Bottom `81:508`, Tab `81:357`, Header `37:477` | `app/oquturbo/shared`: OquTurboNavigationBar, граф; `core/ui`: PageHeader/AppTopBar | 02, 06 v2; H12-03 |
| Сегодня: H01–H08 ready/new/partial/completed/return/loading/error/secondary; H09/H16 expanded, H10 starting, H11 retry | Home/NextExercise `47:297`, PlanEntry `47:302`, CompactFeedback `47:306`, PlanProgress `81:521`, Button | `feature/home`: HomeScreen, HomeUiState, HomeViewModel, TrainingCard/TrainingItem | [06](ux-home-training-06.md); H12-04 |
| Каталог C01, 10 игр, RU/KK compact, wide; интерактивные состояния | GameItem/Playful `68:284`, Catalog/Art/*, InfoTag `37:46` | `feature/games`: GamesScreen, GamesUiState, TrainingGame | [07](ux-catalog-07.md); H12-05 |
| C02 режимы; C03/C07 Custom valid; C04/C05 invalid; длины 2–10 | ModeItem/Playful `68:321`, Catalog/Example `68:338`, SelectableChip `37:43`, Button | `feature/remembernumbermenu`: CustomOptionsDialog, UiState/ViewModel; `core/ui`: GameMenuItem | 07; H12-06 |
| Number Sprint P01–P07: Ready/show/input/correct/error/comparison/record; Binary и Custom 2–10; длинный ввод | Play/Context `87:1684`, Metrics `87:1693`, NumberStage `87:1702`, Comparison `87:1709`, DigitCell `130:573`, SessionHUD `146:650` | `feature/remembernumber`: Screen/TextField/ViewModel; `core/ui`: GameHeader, GameResultCard, GameScoreBadge, GameStateOverlay | [08](ux-play-result-08.md), [поправки 11](ux-validation-11.md); H12-07 |
| Daily P08–P12: Ready, цель не достигнута, pending, зачёт, последний пункт | Play/Goal `87:1716`, Context/Metrics, Button | `feature/remembernumber`, `feature/kenkozgame`, `feature/baspagame`; `core/data` training gate | 08; H12-01/08 |
| Memory Grid M01–M03, показ/воспроизведение, compact/dark/wide V02/V03 | Play/GridCell `89:1414`, общая оболочка/метрики | `feature/memorygrid`, `feature/memorygridmenu`; `core/ui` | 08, 11; H12-09 |
| Прогресс G01–G04: new/empty/one/several; G05 серия; G06 день; G07 попытка; G08 неделя; all-time/legacy/unknown | Progress/Summary `93:555`, Chart `93:668`, HistoryRow `93:580`, DayRow `93:605` | `feature/stats`: RepositoryStatsDataSource, StatsModels, StatsDetailUiState, screens, WeeklyReviewDataSource; `core/data` read-only sources | [09](ux-progress-09.md); H12-10/11 |
| Профиль P01/P02; P03 достижения, P04 оформление; edit/ranks/titles; loading/error не равны new | Settings/Row `105:550`, Input `37:32`, InlineFeedback `37:61`, Button | `feature/profile`: ProfileChildScreens, ProfileViewModel, ProfileStateMapper, ProfileHeroCard | [10](ux-profile-settings-10.md); H12-12 |
| Настройки P05; язык P06 + EN/KK/System; тема P07/light/dark; sound/vibration | Settings/Row, SelectableChip, Button | `feature/profile`; `feature/main`: MainScreen/MainViewModel/LocalAppLocale; `core/data`: SettingsRepository | 10; H12-13 |
| Напоминания: Off/NeedsTime, loading/saving/requesting, blocked/requestable, scheduled/limited, read/schedule/cancel errors, unconfirmed, unsupported | Settings/Row, Input, InlineFeedback, Button | `feature/profile`: ReminderSettingsCard; `core/data/reminders`; `app/oquturbo/shared/reminders` adapters | 10, состояния `108:182`–`108:372`; H12-14 |
| Общие loading/error/empty, disabled/focus, Light/Dark, длинные подписи | Button `36:34`, Input, Surface, EmptyState `37:65`, LoadingState `37:69` | `core/designsystem`, `core/ui`; `resources` | [05](ux-foundations-05.md) с приоритетом v2; H12-02 |

## Контракты поведения, ошибок и возврата

### Навигация и Сегодня

Корни сохраняют выбранный раздел и scroll. Вложенные настройки возвращают к открывшему их
корню; язык/тема не сбрасывают источник. Прогресс сохраняет период, точную серию, день и scroll.
В игре и вложенных страницах bottom bar скрыт. Leaf UI получает callbacks, не NavController;
использовать существующие type-safe routes и Koin. Изменения аргументов маршрутов требуют
отдельного согласования по AGENTS.md; сначала использовать состояние существующего стека.

Следующий пункт — первый незавершённый из реального плана. Количество сегментов вычисляется
из плана, не фиксируется равным трём. Start disabled только при неготовом плане/запуске;
secondary loading/error не блокирует игру. RetryDailyTraining, retryPersonalResult,
retryPracticeHistory повторяют свой запрос. Ошибка может повториться; таймер Figma не SLA.
Раскрытие плана не запускает упражнение, expanded state и возврат фокуса сохраняются.
«Продолжить» запускает следующий пункт, не обещает восстановить незавершённую попытку.

### Выбор и игровой цикл

Каталог: карточка — одна цель нажатия; навыки не кнопки. Для игр без меню — прямой Ready.
Custom: длина 2–10, минимум две выбранные цифры; invalid отключает запуск и даёт текст
восстановления. Закрытие сохраняет выбор в текущем ViewModel, не гарантирует process restoration.
Конфигурация нормализуется существующим sessionMode(): 4/0123456789 = Classic, 4/01 = Binary,
остальные = Custom length:N;digits:S. Retry сохраняет эту идентичность.

Ready → Start → показ → ввод → верно/следующий стимул либо итог. Проверка ответа следует
существующему автоматическому вводу; новую кнопку Submit не добавлять. Фон Ready/result
не запускает игру. «Ещё раз» ведёт через Ready. Выход из активной попытки вызывает текущий
back callback, без новой универсальной паузы/диалога; незавершённая попытка не становится записью.

Число показывается и вводится **в одном и том же поле**. При переходе оно очищается,
принимает ввод и сохраняет положение каждой ячейки. SessionHUD содержит выход, счёт и рекорд;
под ним короткая строка режима, дневной цели или «✓ Верно». В активной игре нет счётчика
введённых цифр и пояснений диапазонов. На compact Ready V01 нет прежней длинной цепочки
инструкций: пример и краткая инструкция, закреплённый Start. NumericKeyboard `137:592` — иллюстрация системного IME,
не требование собственной клавиатуры. Не копировать 248 px как высоту IME на все устройства.
Поддержать цифры, удаление, paste, аппаратный ввод согласно существующему полю.
Длинные ответы не обрезаются; сравнение читает позицию, ожидаемое, введённое, верно/ошибка.

Score, correctAnswers/XP, рекорд серии и дневная цель различаются. XP = correctAnswers,
уровень = floor(totalXp/500)+1; ранги группируют пять уровней. Никаких бонусов за переход.
Свободная игра не завершает дневной пункт. Дневной Next доступен только после подтверждённого
зачёта. Pending не выдаёт подтверждённый XP/выполнение; выход к плану остаётся доступен.
Последний подтверждённый пункт открывает завершённый план, не новое упражнение.

Ошибка записи результата/профиля — **неопределённый контракт D-014/V-09**: нет готового safe retry API.
Не изображать успешную запись по таймеру, не закрывать редактор как сохранённый при ошибке.
H12-00 сначала определяет идемпотентность, частичный успех, повтор и восстановление;
UI safe retry и обещание сохранения до этого заблокированы. Новые форматы хранения не разрешены.

### Прогресс, профиль и настройки

История/график — observeSessions(), all-time — observeTotals(), рекорды — observeRecords().
График: одна точная game/mode/variantId; X — номер попытки, Y — очки; все значения доступны текстом.
Нет попытки ≠ попытка с нулём. До trackingStartedEpochDay — «Нет данных»; позже без события —
«Не записано». Legacy-рекорд не получает выдуманную дату. При усечённом журнале (до 1000 записей)
итог периода подписан «В доступном журнале». Не ограничивать график пятью RecentActivity.
Детали используют временный ключ снимка, учитывающий совпадение timestamps; persisted ID не менять.
Word Flow variants разделяются по языку. Недельное сравнение 5+5 за 28 дней не равно сравнению
средних соседних 7/30-дневных окон. Расчёты и окна полностью заданы в 09.

Профиль не обнуляет legacy XP из-за отсутствия journal. Награды/оформление следуют текущим
условиям mapper, не иллюстрациям. Рамка «Исследователь» сейчас не открывается; скрытое достижение
не имеет реализованного условия. Не обещать их получить. Даты показывать только подтверждённые.
Имя trim-ится, пустое = null; новых лимитов не вводить. Имя/аватар сохраняются кнопкой;
язык/тема применяются сразу. System есть только у языка; у темы два существующих режима.
Звук/вибрация сохраняются как предпочтения без действующих игровых consumers — это сообщается.

Напоминания: Android/iOS supported; JVM/Web unsupported. Enabled preference не равно Scheduled;
Scheduled означает принятие системой, не гарантию доставки. После системного экрана refresh.
Ошибка отмены сохраняет предупреждение о возможном уведомлении и блокирует другие изменения.
Unknown не равно Off, старое известное расписание не затирается выдуманным новым.
Локальное время напоминаний отличается от UTC-дней статистики. Полная таблица состояний — 10.

## Визуальные и поведенческие изменения

| Визуальные (смысл данных прежний) | Поведенческие (нужны отдельные проверки) |
| --- | --- |
| Noto Sans, семантическая палитра, радиусы, декоративные мотивы | 3 корня вместо 4, профиль вложен, контекстный Back |
| Компактная иерархия Сегодня, сегменты плана, игровые карточки | Раскрытие плана; локальный retry не блокирует готовый старт |
| Индикатор выбранной вкладки, focus/pressed, перенос текста | Явный Start/Retry вместо tap-anywhere; повтор через Ready |
| Стабильное поле числа, HUD и текстовые метрики | Custom отдельным экраном; stepper вместо slider (D-034 proposed) |
| Светлые/тёмные и широкие варианты общих компонентов | Точная серия/попытка в Stats, сохранение выбора, текстовые значения графика |
| Ошибка отмечена словом и цветом | Дни единым списком, выбор темы отдельной страницей, честные capability-статусы |

Stepper — предложение UI-контрола, не новая механика; H12-06 подтверждает его до реализации.
Векторные обложки не меняют поля игр. Новые игровые правила, награды, пауза для всех игр,
storage keys/formats и новые зависимости не входят в пакет.

## Backlog небольших задач

Порядок по зависимостям; одна строка — один ограниченный PR или дизайн-возврат. «Можно начать»
означает достаточный handoff для указанной части, а не одобрение всего дизайна пользователем.
Каждая задача включает применимые RU/EN/KK + AppResource, focused tests и screenshots,
если меняются поведение/материальные состояния UI. Проверки сборки — по QUALITY_GATES.

| ID / область | Входы и зависимости | Результат и критерий проверки | Gate |
| --- | --- | --- | --- |
| H12-00 · Контракт записи | 08 D-014, 10 editor, V-09 | Описать ошибки до/после записи, повтор/выход/перезапуск; доказать тестами отсутствие двойного XP, записи и зачёта. Согласовать UI failure states и потребность изменения storage отдельно | Блокирует safe retry и полную приёмку H12-08/12 |
| H12-01 · Дневной прототип и Back | 06/08/10, V-04–06 | Добавить настоящий сценарный успех к P08, продолжение H02 через согласованный Wide Eye, возврат H04 и из настроек к каждому источнику/локали. Проверить viewer: начало → завершение → возврат без demo-подмены | Дизайн-возврат; не новая концепция |
| H12-02 · Тема и базовые действия | 05 + приоритет v2 | Обновить OquTurboTheme, Button/Header/feedback; подтвердить поставку/лицензии шрифтов и RU/KK glyphs. Focus 2 px / offset 4, цели ≥48, CTA ≥56, Light/Dark и 150% screenshots | Можно начать |
| H12-03 · Корневой shell | H12-02, 02, Navigation/Bottom | Три вкладки; настройки из каждого корня; профиль из Прогресса. Back сохраняет источник/scroll/локаль. 320/150% горизонтально, inset вне scroll | Навигационная приёмка зависит от H12-01 |
| H12-04 · Сегодня | H12-02/03, H01–H11/H16 | Все ready/loading/error/partial/completed states; secondary failure не блокирует Start; повторный Start исключён; количество сегментов из плана. На 390 CTA виден, 320/150% доступен без перекрытия | Можно начать UI; E2E после H12-01/08 |
| H12-05 · Каталог | H12-02/03, C01/stress, GameItem/Playful | Все 10 игр и прежние routes; одна click-цель, текстовые навыки; 2 колонки на 390, 1 при compact/150%; полный EN/KK каталог, focus/pressed | V-07 закрыть локальной матрицей |
| H12-06 · Number Sprint меню/Custom | H12-02/05, C02–C07/L2–L10 | Подтвердить D-034; границы 2/10 disabled, 0/1 цифра invalid, исправление без потери выбора. Тесты нормализации Classic/Binary/Custom; Back без запуска | Stepper proposed, не молчаливое изменение |
| H12-07 · Number Sprint free loop | H12-02/06, P01–P07, Binary/Custom, поправки 11 | Явные Start/Retry, неактивный фон, стабильное поле, auto input, сохранённая серия повтора. Проверить 2/4/10 цифр, удаление/paste/IME, timer/background/exit; screenshots Ready/input/error/record/dark | Можно начать; safe retry исключён до H12-00 |
| H12-08 · Дневной результат | H12-00/01/04/07, P08–P12 | Ни pending, ни free play не дают зачёт; confirmed Next ровно один раз; последний пункт → H03; повтор не меняет контекст; failure/restart не удваивают XP | Blocked до контрактов |
| H12-09 · Общая оболочка / Memory Grid | H12-02/07, M01–M03 + V02/V03 | Мигрировать overlay с явным действием; Route/Reverse/Flash сохраняют порядок, размер поля, timers и счёт; semantics клетки row/column/state. Проверить всех consumers core/ui | Не переносить поля Number Sprint |
| H12-10 · Stats UI-проекции | 09, G01–G08, GameSession | Проекции exact series, dates/answers/duration, временный ключ попытки. Тесты 0/1/many, duplicate timestamp, unknown variant, Word Flow locale, legacy, 1000+ totals, UTC boundary | Без изменения persisted IDs/formats |
| H12-11 · Progress UI | H12-02/03/10, G01–G08 | Один график + текст, 7 дней доступны единым scroll, period/series/day сохранены на Back; loading/error независимы, нет выдуманных дат/нулей. Dark/320/150%/wide screenshots | Можно после проекций |
| H12-12 · Профиль | H12-02/03/11, 10 P01–P04/edit/ranks | Новичок/legacy/прогресс; XP и награды по mapper; locked честно, editor не закрывается при failure; пустое имя null. Проверить сохранение/отмену и недоступный avatar | Error save после H12-00 |
| H12-13 · Язык/тема/предпочтения | H12-02/03, P05–P07, sound/vibration, V-06/07 | System/EN/RU/KK, две темы, immediate apply и restart persistence; вложенные экраны полностью переводятся, Back сохраняет источник. Sound/vibration не обещают игровой эффект | Полная локализация требует редактуры |
| H12-14 · Напоминания UI | H12-13, все states 10 | Отрисовать capability/authorization/actual schedule; ошибки read/schedule/cancel/settings-open, возврат refresh; Android/iOS native acceptance и JVM/Web unsupported | Device evidence обязательно |
| H12-15 · Остальные игры | H12-09 + карта исключений ниже | Отдельный PR на игровое семейство; явные действия/метрики без изменения stimuli/pause/score; проверка каждой standalone сборки | Не один массовый непроверенный PR |
| H12-16 · Сквозная приёмка | Все применимые задачи, 11 matrix | Viewer + Android и доступные другие targets: 3 сценария, Back, locale/theme, IME, font scaling, screen reader, keyboard, error/restart. Для отсутствующих платформ explicit unverified. Языковая редактура RU/EN/KK | Только evidence снимает V-04–09 |

## Игровые исключения и потребители

| Игра / модуль | Что сохранить при переносе оболочки |
| --- | --- |
| Number Sprint / remembernumber | Автоматическая проверка ввода, нормализованная конфигурация; без новой Pause |
| Memory Grid / memorygrid | Route по порядку, Reverse обратно, Flash все клетки без повторов; собственные поля/таймеры, без дневного Goal |
| Wide Eye / kenkozgame | Периферические стимулы, четыре режима; дневной Goal допустим; не переносить сравнение цифр |
| Don’t Tap / baspagame | Семь режимов, togglePause, таймер реакции; правильное ненажатие увеличивает correctAnswers без score; XP не равен score |
| Word Flow / wordflow | Слова/отвлечения, locale variantId, таймер и pause/resume; без дневного зачёта |
| Dual Focus / dualfocus | Две параллельные задачи и собственная пауза; без дневного зачёта |
| Rotation Match / rotationmatch | Геометрия и pause/resume; без дневного зачёта |
| Number Trail / numbertrail | Порядок чисел, elapsed timer и pause/resume; без дневного зачёта |
| Symbol Count / symbolcount | Фигуры, варианты ответа и pause; без дневного зачёта |
| Rule Switch / ruleswitch | Текущее правило, stimuli и pause; без дневного зачёта |

Сборка OquTurbo потребляет все общие игры. Standalone есть у Number Sprint, Wide Eye,
Don’t Tap, Word Flow, Rotation Match, Number Trail, Symbol Count, Rule Switch.
Для изменения core/ui проверить каждого затронутого потребителя по settings.gradle.kts,
а не только OquTurbo. Новые поля остальных игр в Figma не спроектированы.

## Проверки этой передачи и оставшееся неизвестное

27 октября прочитаны 06–11, PRODUCT.md, QUALITY_GATES и текущие слои Figma.
`git diff 28fe2af HEAD -- app core feature resources PRODUCT.md` пуст: кодовые свидетельства
исходного аудита и последующих handoff не устарели из-за изменений этих каталогов.

Структурная проверка восьми страниц (04, 06_Components, 07–12): у просмотренных instances
нет потерянного main component. Разрешены 175 уникальных внутренних целей NODE/URL,
отсутствующих IDs нет. Проверены 108 local variables в двух коллекциях: нет одинаковых
имён внутри коллекции и отсутствующих локальных alias targets. Это не аудит всех hardcoded
paints, override differences или визуальной согласованности каждого исторического экрана.
Архивация сохраняет node IDs; legacy masters не detaching/deleting.

Статические renders, контраст и stress-матрица 11 остаются их собственным evidence;
эта задача не объявляет их новым runtime-тестированием. Навигатор 13 просмотрен статически: шесть подписанных кнопок 616×56, без обрезки текста.
После архивации повторно проверены Catalog (458 instances), Components (268) и Profile (265):
нет потерянных masters.
Нет новых тестов с участниками, Presentation viewer walkthrough, реальных IME/таймеров,
screen reader, platform text scaling, доставки уведомлений и перезапуска с записью.
V-07: недостающие EN-каталог/длинные режимы и вложенные переводы остаются в H12-05/13/16;
реестр строк не заменяет их визуальную проверку. Переводы — draft, особенно KK требует редактуры.

Открыто: пользовательская оценка направления v2; обнаруживаемость профиля/раскрытия;
D-034 stepper; D-014 запись; шрифты/лицензии на targets; реальный context Back;
полнота локализации и платформенная доступность. Не маскировать вопрос новым fixture.

Gradle/unit/Roborazzi неприменимы к этому diff: production и baselines не менялись.
Проверка репозитория: локальные ссылки документов, полнота RU/EN/KK и placeholders реестра,
`git diff --check`, просмотр diff/status. Проверка дала PASS: 23 локальные ссылки и 59 трёхъязычных строк с совпадающими placeholders.
Результаты публикации указаны в PR.
