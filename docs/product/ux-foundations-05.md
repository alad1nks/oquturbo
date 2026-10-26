# Задача 05 — основа дизайн-системы

Дата: 25 октября 2026. Статус: **Ready** по критериям дизайн-задачи.
Направление B «Ясная практика» остаётся **working**, не пользовательским одобрением.
База: актуальная `main`, `657f5d0c3cb6e78b1985d13d7c9d75a6d742acea`; выполнены fetch и fast-forward pull.
Работа сделана на `main` по указанию пользователя. Продуктовый код не менялся.

## Результат

[Handoff и журнал на 00_Project](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=43-2).

- [05_Foundations](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=38-2): Light/Dark, семантические роли, типографика, геометрия и правила.
- [06_Components — образцы](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=38-133): связанные instances всех 11 базовых семейств.
- [Контракты и карта кода](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=42-121).
- [Stress-образцы](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=40-88): RU/KK 320×640 при 150%, EN, Dark, 800×1100.
- [Макеты 04](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=28-139): B переведён на компоненты; A сохранён как альтернатива.

Исходные IDs B-кнопок и variables сохранены: система развивает существующие assets, а не создаёт
параллельную палитру. Все 12 B-screen frames (основные и stress-варианты) используют Header/Navigation
instances и общие действия: 66 instances суммарно. Структурные контейнеры, текстовые данные и игровое
сравнение ответов остаются редактируемыми слоями; создавать универсальную карточку результата вне
задачи 08 не стали. A продолжает использовать свои исходные instances и архивные цветовые роли.

## Проверка задач 01–04 до начала

Прочитаны план, PRODUCT.md, четыре handoff-документа и актуальный журнал 00_Project.
Страницы и редактируемые материалы подтверждены непосредственно в Figma.
Между базой аудита 01 (`28fe2af`) и текущей main нет изменений в app/core/feature/resources/PRODUCT.md:
кодовые свидетельства предыдущих задач не устарели из-за изменений этой базы.

| Задача | Проверено | Вывод |
| --- | --- | --- |
| 01 | Три задания, карта пяти путей, семь приоритетных проблем, различение факта и гипотезы, неизвестное; 7 листов и 119 текстовых узлов | Ready; обязательных доработок в объёме 01 не найдено |
| 02 | Две структуры, рабочая архитектура Сегодня/Игры/Прогресс, inventory F01–F26, настройки/профиль, источник возврата; 7 листов, 122 текстовых узла | Ready; функции и контексты не потеряны |
| 03 | 12 семейств, отдельные daily/free/pending, retry через Ready, ошибки и возвраты; повторно проверены 128 реакций | Ready; отсутствующих destinations и click-зон меньше 48×48 нет |
| 04 | Четыре макета визуально просмотрены; A/B различаются композицией и типографикой, fixtures одинаковые; рекомендации, stress-листы и журнал присутствуют | Ready; B остаётся working, обязательная доработка перед 05 не требуется |

Это экспертная проверка материалов и графа Figma, не пользовательское исследование и не ручной проход
Presentation viewer. Открытые вопросы обнаруживаемости профиля, повторного Ready, восстановления записи,
шрифтов и платформенной доступности остаются в своих задачах. Они не объявлены решёнными в 05.

## Переменные и типографика

Коллекция `DIR04 / Explorations` переименована в `OquTurbo / Foundations`, IDs и режимы Light/Dark сохранены.
Прежние `B/*` стали `color/*`, `A/*` — `archive/A/*`. Семантические цвета ссылаются на primitives.
Существующие primitives и интервалы переиспользованы; новая коллекция с дублирующими значениями не создавалась.
WF03 остаётся самостоятельной черновой grayscale-основой прототипа, а не вторым рабочим направлением.

| Роль | Light | Dark |
| --- | --- | --- |
| color/bg | #FAF8F2 | #151C1A |
| color/surface | #FFFFFF | #202B27 |
| color/text | #172B25 | #F2F5EF |
| color/text-secondary | #4D6259 | #B8C9C0 |
| color/action | #006B54 | #83D5B4 |
| color/on-action | #FFFFFF | #102D23 |
| color/action-pressed | #005440 | #A0E6CA |
| color/selection | #DCEEE5 | #314C40 |
| color/disabled-bg | #E0E5DF | #35423B |
| color/disabled-text | #53645A | #AEC0B3 |
| color/border | #687B70 | #91A99A |
| color/divider | #C9D2CC | #45554D |
| color/focus | #006B54 | #83D5B4 |
| color/success | #236540 | #9ED6AF |
| color/error | #A32B24 | #FFB4AB |

Divider — декоративный разделитель; границы обязательного поля используют border.
Success описывает выполнение цели; error — неверный ответ или сбой. Не окрашивать весь результат одной оценкой.

12 текстовых стилей `OquTurbo/{role}/{1|1.5}` получены переименованием B-стилей, сохраняя их IDs.

| Роль | Семейство / начертание | Размер / интерлиньяж при 100% |
| --- | --- | --- |
| title | Noto Serif Regular | 32 / 40 |
| game | Noto Sans Medium | 22 / 28 |
| body | Noto Sans Regular | 16 / 24 |
| caption | Noto Sans Regular | 14 / 20 |
| action | Noto Sans Bold | 16 / 24 |
| number | Noto Sans Bold | 48 / 56 |

При 150% увеличивается текст, не viewport и не все интервалы. Игровые стимулы и числа — без засечек.
Noto доступен в Figma; поставка файлов, лицензий и глифов во все targets остаётся задачей реализации.
CSS identifiers `var(--oq-*)` — предлагаемые имена handoff, не существующий CSS API Kotlin-проекта.
Code Connect для проверенных модулей не найден; новые mappings не создавались, поскольку API ещё не реализован.

## Геометрия и поведение

- Боковой отступ 24, при 320 px — 16; интервалы 4/8/12/16/24/32. Значение 20 сохранено для альтернативы A.
- Радиусы: 0 для плоских групп, 8 для контролов/вставок. Тени не нужны.
- Primary CTA от 56; прочие действия от 48×48; icon 24. Это минимумы, высота растёт по содержимому.
- Auto Layout, FILL по ширине, HUG по высоте; текст HEIGHT без ellipsis для названия, цели, ошибки и CTA.
- Focus: внешняя обводка 2 px с зазором 4; родитель не обрезает её. Keyboard focus в приложении не проверен.
- Selected: подчёркивание или ✓ дополнительно к цвету; InfoTag — простой текст без состояний кнопки.
- Disabled/loading не активируются. Error сопровождается осмысленным текстом; retry — отдельное действие.
- Surface и фон результата не запускают игру. Header хранит выход согласно источнику, Navigation скрыта в игре.
- Реальные системные insets определяет платформа. Для 320/150% используются stacked chrome и вертикальная прокрутка.
- Широкая колонка: старт до 640, игра/результат до 560; не растягивать стимулы по всему окну.

## Компоненты и применимость состояний

На 06_Components: **12 component sets, 51 variant и 1 icon master**. Одиннадцать обязательных семейств
дополнены внутренним NavigationItem и сменяемой иконкой. Нет варианта на каждый icon: используется INSTANCE_SWAP.

| Семейство | Варианты / свойства | Неприменимые состояния |
| --- | --- | --- |
| [Button](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=36-34) | Primary/Text × Default/Pressed/Focus/Disabled/Loading × TextScale 1/1.5; Label | Error показывается соседним InlineFeedback |
| [IconButton](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=37-84) | Default/Pressed/Focus/Disabled; Icon swap; accessible name задаёт вызывающий код | Loading/error не нужны действию выхода |
| [Navigation](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=37-500) | Row/Stacked; nested Button/Text и selected NavigationItem с подчёркиванием; состояния действий через nested variants | Загрузка плана не отключает разделы |
| [Header](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=37-477) | Root/Session × Row/Stacked; Title и nested действия | Собственных loading/error нет, только состояния вложенных действий |
| [Surface](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=37-51) | Plain/Inset; Content | Неинтерактивна: pressed/focus/disabled неприменимы |
| [Input](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=37-32) | Default/Pressed/Focus/Disabled/Loading/Error; Label, Value, Hint | Не определяет диапазон значений или игровую клавиатуру |
| [SelectableChip](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=37-43) | Default/Selected/Pressed/Focus/Disabled; Label; selected подчёркнут | Loading/error не нужны локальному выбору |
| [InfoTag](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=37-46) | Default; Label | Нет selected/pressed/focus/onClick |
| [InlineFeedback](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=37-61) | Info/Success/Error; Title, Message | Сам блок не действие; экран обязан передать текст события |
| [EmptyState](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=37-65) | Default; Title, Message; действие размещается отдельно | Пустота не является error/loading |
| [LoadingState](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=37-69) | Default: текст и indeterminate-образец | Нет вымышленных процентов или обещанной длительности |

Состояния представлены статически. Input specimens проверяют переносы, а не утверждают ошибочность числа 4
или новые правила Custom. Для error экземпляра вызывающий экран задаёт Hint, для InlineFeedback — Title/Message;
общий default property не является готовым текстом конкретного сбоя. EN/KK stress-copy — draft.

## Карта существующего кода

Пути компонентов ниже относятся к `core/ui/.../component`, если не указано иначе.
Классификация описывает будущую реализацию, а не уже сделанные изменения Kotlin.

| Дизайн / существующий код | Решение | Что требуется |
| --- | --- | --- |
| Tokens / OquTurboTheme, OquTurboLayout, OquTurboColors | modify | Обновить существующую тему, не вводить параллельную; решить поставку шрифтов |
| Button / Material3 Button, TextButton в features | modify | Единый стиль, focus/loading и перенос подписи |
| IconButton / AppBackButton | modify | 48×48 сохранить, icon 24, localized contentDescription |
| Navigation / app/oquturbo/shared/.../OquTurboNavigationBar | modify | Архитектура 02, selected/pressed/focus и компактный вариант |
| Header / PageHeader, AppTopBar, GameHeader | modify | Иерархия B; перенос вместо однострочного ellipsis важного контекста |
| Surface / AppCard, AppBackground | modify | Плоская/вставная поверхность, без теней и ложной интерактивности |
| Input / Material3 OutlinedTextField в ProfileChildScreens | modify | Использовать существующий примитив; label/helper/error и токены |
| SelectableChip / FilterChip в StatsSections, RememberNumberMenuCustomOptionsDialog | reuse | Сохранить selection contract, адаптировать тему/минимальную зону |
| InfoTag / GamesScreen.SkillChip | replace | Текстовая информационная метка вместо псевдофильтра |
| InlineFeedback / локальные сообщения Home/игр | new | Небольшой общий текстовый блок; retry отдельным Button |
| EmptyState / StatsScreen.EmptyStateCard и локальные Home states | new | Общая основа с содержимым, принадлежащим экрану |
| LoadingState / локальные CircularProgressIndicator + Text в Home/Stats | new | Общая основа без блокировки готового первичного содержимого |
| GameMenuItem | modify | Новая композиция и типографика в задаче 07 |
| GameStagePanel | reuse | Сохранить layout-контракт; внешний вид получает новую тему |
| GameScoreBadge, GameResultCard | modify | Счёт/цель/XP раздельно, игровые составные варианты в 08 |
| GameStateOverlay, AnimatedGameStateOverlay | modify | Убрать tap-anywhere, добавить явные действия по D-003 |

## Проверки и ограничения

| Проверка | Свидетельство |
| --- | --- |
| Variables | Нет повторяющихся имён в коллекции, ALL_SCOPES и пропущенного WEB code syntax; семантика через aliases |
| Paints | На странице компонентов нет непривязанных solid fills/strokes у UI-узлов; рамки component sets исключены как editor chrome |
| Типографика | Все 140 текстовых узлов проверенных gallery/stress/mapping-листов имеют text styles |
| Контраст | 20 пар sRGB в Light/Dark: проверенные текстовые пары ≥4,93:1; существенные border/focus ≥4,51:1. Divider исключён как декоративный |
| Зоны | Button/IconButton/SelectableChip/NavigationItem masters ≥48×48; все 48 реакций 12 B-screen frames без маленьких целей и потерянных destinations |
| Основной старт | CTA заканчивается y=493, nav начинается y=746 на 390×844; значения сохранены после миграции |
| 320×640 / 150% | RU/KK component specimens: content 608/578 при viewport 640; overflow VERTICAL; тексты и кнопки переносятся. B_Start compact тоже просмотрен, CTA ниже первого viewport доступен через заданную прокрутку |
| EN/KK, Dark, Wide | Статически просмотрены component specimens, KK-результат B, wide 800×1100; прежние stress-макеты B сохраняют локали и геометрию |
| Редактируемость | Компоненты, instances, текст и Auto Layout; UI не заменён изображениями |
| Геометрия | После исправлений перекрытий в проверенных вертикальных specimen-группах нет |

В ходе визуальной проверки исправлены обрезание Auto Layout в служебных листах, жёсткая ширина nav,
неподходящие default property значения в образцах состояний и разрыв длинного RU-заголовка на 320/150%.

Не проверялись реальные жесты прокрутки, клавиатурный focus, screen reader, back, IME, таймеры,
сохранение/повтор записи и платформенная поставка шрифтов. Figma не заменяет эти проверки.
Нет языковой редактуры носителями EN/KK, исследования с участниками или одобрения направления B.
Gradle/Roborazzi не запускались: изменения репозитория только документальные, PNG и Kotlin не менялись.

## Решения и передача

D-001–D-020 сохраняются; новые решения не имеют статуса validated.

| ID | Статус | Решение / основание / альтернатива |
| --- | --- | --- |
| D-021 | working | Продолжить B, сохранив исходные IDs tokens/actions; отдельная новая палитра создала бы две несогласованные основы |
| D-022 | working | 11 минимальных семейств, states только по применимости; универсальные игровые составные компоненты отложены до 07/08 |
| D-023 | working | Вся серия B использует общие действия/Header/Navigation; A и WF03 сохранены как исследовательские материалы, не удалены |
| D-024 | proposed | Реализовать через текущие OquTurboTheme/core/ui; сначала подтвердить Noto по targets, затем переносить экраны. Новый независимый UI framework не нужен |

Задаче 06 передаются foundations, variants, карта кода, связанные макеты B и ограничения 03/04.
Нужно спроектировать состояния Главной/плана и локальные ошибки, сохраняя отдельные цель, результат,
XP, pending записи и контекст запуска. Вторичная загрузка не должна блокировать готовый CTA.
Не переносить служебные specimens в продуктовый UI и не считать дизайн реализованным.
