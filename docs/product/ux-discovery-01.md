# Задача 01 — диагностика пользовательского пути

Дата: 25 октября 2026. Статус: **Ready** в смысле критериев задачи, не одобрения дизайна.
База: `main`, `28fe2af66fc75f5edc7a204c55098996abd31770`; `git pull --ff-only origin main` выполнен.

[Figma — 00_Project](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=4-2)

- [Метод и поправки](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=3-2)
- [Три задания](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=3-20)
- [Карта сценариев](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=3-32)
- [Проблемы P0](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=3-50)
- [Проблемы P1](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=3-65)
- [Неизвестное](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=3-84)

## 01 / Диагностика пути

OquTurbo • 25 октября 2026 • main 28fe2af

### Статус: Ready — экспертный аудит

Критерии задачи 01 выполнены по коду и статическим превью. Ready не означает одобрение пользователя или доказанную эффективность. Детальные макеты, палитра и новые функции вне объёма.

### Уровни доказательности

ФАКТ / КОД — структура и логика реализации.
ФАКТ / PNG — только видимое тестовое состояние.
ГИПОТЕЗА — ожидаемое затруднение, требует проверки.
ПРЕДЛОЖЕНИЕ — направление следующей задачи, не утверждённый дизайн.

### Что действительно проверено

Обновление main fast-forward до 28fe2af66fc75f5edc7a204c55098996abd31770. PRODUCT.md; Home/Games/Stats/Profile; настройки; меню и результат Number Sprint; маршруты дневного плана.
Просмотрены HomeFreshTrainingPreview, GamesScreenRussianPreview, RichStatsPreview, RememberNumberScreenMistakePreview, ProfileNewUserPreview, RememberNumberMenuScreenPreview.

### Поправки к исходному аудиту

UX-10: текущий home_title = Daily Mind во всех трёх локалях, не Turbo Reading. UX-03: loading/error/retry тренировки уже реализованы; готовый CTA не зависит от вторичной статистики. Отсутствие этих состояний не подтверждено.
Spinner на PNG не доказывает зависание. Длинный leaf-preview не равен viewport с нижней навигацией. Седьмой день скрыт по ширине, а не удалён из данных.

### Runtime

status: нет устройств, boot_completed=0. Попытка start с timeout 25s: software emulator без KVM; готовый runtime не получен. Поведение приложения не проверено взаимодействием. Старые ограничения pull из плана больше не актуальны.

## 02 / Три пользовательских задания

Будущие проверочные задания • не результаты исследования

### T1 • Заняться сегодня

«Вы впервые открыли приложение. Начните сегодняшнее занятие, после ошибки попробуйте ещё раз, выполните цель и продолжите план».

Успех для будущей проверки: без подсказок находится старт, различаются цель/счёт, повтор и следующее упражнение; возвращение сохраняет частичный план. Это критерий, не результат теста.

### T2 • Выбрать свободную игру

«Найдите Числовой спринт, выберите Classic, сыграйте, прочитайте результат и вернитесь к режимам. При необходимости откройте Custom».

Успех: человек объясняет выбранный режим и следующее действие; свободная попытка не засчитывается как пункт дневного плана.

### T3 • Понять свой прогресс

«Посмотрите активность за неделю и результат выбранной игры. Объясните, с чем он сравнивается. Затем найдите язык и тему».

Успех: различаются отсутствие данных, ноль и рекорд; доступны все 7 дней; сравнение внутри одной игры/режима/конфигурации; настройки находятся без помощи.

## 03 / Карта текущих сценариев

Намерение → действие → реакция системы → затруднение

### Первый запуск

Хочу начать → Главная, CTA тренировки → готовый план или загрузка/ошибка с повтором → уровень и полный список предшествуют старту (A), цель не подписана (C).

Источник: HomeScreen:158–174, :270–340. Состояние новичка реконструировано по preview, реальный первый запуск не выполнен.

### Дневное занятие

Хочу выполнить план → старт следующего упражнения → маршрут с trainingEntryId/requiredScore → Ready → показ → ввод → ошибка/результат → повтор до цели → продолжение → завершение плана.

Источники: App.kt:297–328; RememberNumberScreen:199–237; PRODUCT.md. Риск B/C; выход и сохранение после перезапуска требуют runtime.

### Свободная игра

Хочу выбрать упражнение → Игры → Number Sprint → Classic / Binary / Custom → Ready → игра → результат → повтор или назад к режимам.

Источники: GamesScreen:113; RememberNumberMenuContentColumn:37–66; RememberNumberScreen:199–312. Риски B/D/E; Custom-конфигурации сохраняют идентичность.

### Результат и прогресс

Хочу понять результат → прочитать счёт/рекорд → Статистика → период → день → игра/режим/серия → подробности.

Источники: RememberNumberScreen:266–292; StatsSections:163–210; PRODUCT.md. Риск F. XP = правильные ответы; счёт и XP нельзя считать синонимами.

### Настройки

Хочу сменить язык/тему → Профиль → шестерёнка → язык (диалог) / тема (переключатель) → назад.

Источники: ProfileScreen:149–168; ProfileChildScreens:606–657. Нет подтверждённого дефекта доступности настроек; применение, сохранение и системный back не проверены.

## 04 / Основной сценарий · P0

Приоритет UX, не аварийность • влияние пока гипотеза

### UX01-A • P0 · Начало занятия ниже уровня и списка

Факт [код + PNG]: HomeScreen:158–174 показывает LevelProgress перед планом; :309–325 выводит весь список до CTA. В HomeFreshTrainingPreview кнопка начинается около y=504 из 1100.

Гипотеза последствия: старт сложнее заметить при первом визите. Это не доказательство, что CTA скрыт на 390×844: PNG не включает навигацию приложения.

Предложение: в задаче 02 приоритизировать следующее упражнение; в 03 проверить CTA вместе с системными зонами.

[Свидетельство](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/home/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/home/ui/HomeScreen.kt#L158).

### UX01-B • P0 · Повтор и продолжение запускаются нажатием на фон

Факт [код + PNG]: GameStateOverlay:38–41 задаёт onClick для Surface.fillMaxSize. RememberNumberScreen:204–237 выбирает повтор либо продолжение плана. Mistake-preview показывает «Try Again?» без отдельной кнопки.

Гипотеза последствия: случайный тап прерывает чтение результата; действие и зона нажатия неочевидны.

Предложение: явные действия «Ещё раз» / «Следующее упражнение» и выход. Это изменение поведения, не только оформления.

[Свидетельство](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/core/ui/src/commonMain/kotlin/com/alad1nks/oquturbo/core/ui/component/GameStateOverlay.kt#L29).

### UX01-C • P0 · Очки в плане не названы целью

Факт [код + PNG]: HomeScreen:536–540 выводит TrainingScore; HomeFreshTrainingPreview показывает 8 / 12 / 10 pts без слова «цель». В игре :204–208 условие продолжения — score ≥ trainingRequiredScore.

Гипотеза последствия: пользователь может принять требуемые очки за уже заработанные и не понять условие завершения.

Предложение: явно различить цель и текущий результат, не менять порог или правило зачёта.

[Свидетельство](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/home/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/home/ui/HomeScreen.kt#L536).

## 05 / Поиск и понимание · P1

Четыре наблюдения • всего семь проблем

### UX01-D • P1 · Информационные метки похожи на фильтры

Факт [код + PNG]: GamesScreen:173–210 — SkillsRow и SkillChip с Surface без onClick; в RU-preview это отдельный ряд округлых плашек.

Гипотеза последствия: ожидаемый фильтр не срабатывает; пользователь пробует бесполезное действие.

Предложение: обозначать навыки как информацию; фильтрацию не добавлять без отдельного обоснования.

[Свидетельство](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/games/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/games/ui/GamesScreen.kt#L173).

### UX01-E • P1 · Каталог требует длинного последовательного просмотра

Факт [код + PNG]: GamesScreen:113–121 выводит ActiveGameCard для каждой игры. На PNG 390×1400 полностью видны пять карточек и начало шестой; PRODUCT.md перечисляет десять игр.

Гипотеза последствия: поздние игры сложнее обнаружить и сравнить; частота такого затруднения неизвестна.

Предложение: проверить более компактную подачу всех десяти игр с сохранением названия, описания и доступной зоны нажатия.

[Свидетельство](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/games/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/games/ui/GamesScreen.kt#L113).

### UX01-F • P1 · Недели календаря прокручиваются отдельно

Факт [код + PNG]: StatsSections:180–191 создаёт LazyRow внутри days.chunked(7). На RichStatsPreview в полных неделях справа видны только шесть дней.

Гипотеза последствия: седьмой день трудно обнаружить, горизонтальное положение недель может расходиться.

Предложение: единая недельная сетка либо согласованный доступный список/прокрутка; не менять UTC-группировку.

[Свидетельство](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/stats/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/stats/ui/StatsSections.kt#L180).

### UX01-G • P1 · Профиль новичка занят ещё пустым прогрессом

Факт [код + PNG]: ProfileScreen:102–129 размещает Hero, NewUserHint, Summary и Achievements. ProfileNewUserPreview показывает крупную идентичность/ранг и нулевые показатели.

Гипотеза последствия: информации много до появления личного результата. Настройки при этом уже доступны в шапке (:149–168), их недоступность не подтверждена.

Предложение: проверить компактный вводный профиль; сохранить прямой доступ к языку/теме и честное «недостаточно истории».

[Свидетельство](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/profile/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/profile/ui/ProfileScreen.kt#L102).

## 06 / Неизвестное и проверки

Не превращать допущения в продуктовые факты

### Аудитория и контекст

Неизвестны возраст, опыт, мотив, ограничения зрения/моторики и частота занятий. Рабочая гипотеза: самостоятельные короткие упражнения на телефоне. Проверка: интервью и T1–T3 с новичками и возвращающимися пользователями; не выводить сегменты из кода.

### Продуктовые решения

Не согласованы окончательное имя OquTurbo / Daily Mind, целевая длительность, приоритет платформ и требования доступности. PRODUCT.md не подтверждает когнитивный эффект. Не обещать «5 минут», улучшение интеллекта или новые бонусы.

### Данные и механики

Сохранить XP = число правильных ответов, 500 XP/уровень; рекорды внутри игры/режима/конфигурации; журнал для истории, totals для all-time; UTC. Не создавать даты старых рекордов. В план входят Number Sprint, Wide Eye, Don’t Tap; остальные семь исключены согласно PRODUCT.md.

### Проверки, оставшиеся открытыми

Реальные клики, таймеры, back, прерывание/возврат, сохранение, повтор ошибки, применение языка/темы, уведомления/звук/вибрация, screen reader. Android start запущен с лимитом 25 секунд без KVM; готовый runtime не получен. Это ограничение попытки, а не доказанная невозможность запуска.

### Границы текущего результата

Экспертный аудит кода и 6 PNG, без участников и интерактивного walkthrough. 390×844, 320×640, текст 150%, EN/KK, dark и 800×1100 остаются проверками будущих экранных задач; листы аудита не являются экранами приложения.

## Таблица проблем

| Проблема | Свидетельство | Последствие (гипотеза) | Приоритет |
| --- | --- | --- | --- |
| UX01-A — Начало занятия ниже уровня и списка | [Код](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/home/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/home/ui/HomeScreen.kt#L158) и соответствующий PNG, детали выше | старт сложнее заметить при первом визите. Это не доказательство, что CTA скрыт на 390×844: PNG не включает навигацию приложения. | P0 |
| UX01-B — Повтор и продолжение запускаются нажатием на фон | [Код](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/core/ui/src/commonMain/kotlin/com/alad1nks/oquturbo/core/ui/component/GameStateOverlay.kt#L29) и соответствующий PNG, детали выше | случайный тап прерывает чтение результата; действие и зона нажатия неочевидны. | P0 |
| UX01-C — Очки в плане не названы целью | [Код](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/home/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/home/ui/HomeScreen.kt#L536) и соответствующий PNG, детали выше | пользователь может принять требуемые очки за уже заработанные и не понять условие завершения. | P0 |
| UX01-D — Информационные метки похожи на фильтры | [Код](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/games/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/games/ui/GamesScreen.kt#L173) и соответствующий PNG, детали выше | ожидаемый фильтр не срабатывает; пользователь пробует бесполезное действие. | P1 |
| UX01-E — Каталог требует длинного последовательного просмотра | [Код](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/games/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/games/ui/GamesScreen.kt#L113) и соответствующий PNG, детали выше | поздние игры сложнее обнаружить и сравнить; частота такого затруднения неизвестна. | P1 |
| UX01-F — Недели календаря прокручиваются отдельно | [Код](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/stats/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/stats/ui/StatsSections.kt#L180) и соответствующий PNG, детали выше | седьмой день трудно обнаружить, горизонтальное положение недель может расходиться. | P1 |
| UX01-G — Профиль новичка занят ещё пустым прогрессом | [Код](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/profile/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/profile/ui/ProfileScreen.kt#L102) и соответствующий PNG, детали выше | информации много до появления личного результата. Настройки при этом уже доступны в шапке (:149–168), их недоступность не подтверждена. | P1 |

## Источники

- [home: feature/home/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/home/ui/HomeScreen.kt](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/home/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/home/ui/HomeScreen.kt)
- [overlay: core/ui/src/commonMain/kotlin/com/alad1nks/oquturbo/core/ui/component/GameStateOverlay.kt](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/core/ui/src/commonMain/kotlin/com/alad1nks/oquturbo/core/ui/component/GameStateOverlay.kt)
- [play: feature/remembernumber/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/remembernumber/ui/RememberNumberScreen.kt](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/remembernumber/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/remembernumber/ui/RememberNumberScreen.kt)
- [games: feature/games/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/games/ui/GamesScreen.kt](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/games/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/games/ui/GamesScreen.kt)
- [stats: feature/stats/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/stats/ui/StatsSections.kt](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/stats/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/stats/ui/StatsSections.kt)
- [profile: feature/profile/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/profile/ui/ProfileScreen.kt](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/profile/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/profile/ui/ProfileScreen.kt)
- [settings: feature/profile/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/profile/ui/ProfileChildScreens.kt](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/profile/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/profile/ui/ProfileChildScreens.kt)
- [app: app/oquturbo/shared/src/commonMain/kotlin/com/alad1nks/oquturbo/shared/App.kt](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/app/oquturbo/shared/src/commonMain/kotlin/com/alad1nks/oquturbo/shared/App.kt)
- [menu: feature/remembernumbermenu/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/remembernumbermenu/ui/RememberNumberMenuContentColumn.kt](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/feature/remembernumbermenu/src/commonMain/kotlin/com/alad1nks/oquturbo/feature/remembernumbermenu/ui/RememberNumberMenuContentColumn.kt)
- [PRODUCT.md](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/PRODUCT.md)
- [Home_fresh_training_W390dp_H1100dp](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/screenshot-tests/src/test/screenshots/jvm/com.alad1nks.oquturbo.feature.home.ui.HomeScreenKt.HomeFreshTrainingPreview.Home_fresh_training_W390dp_H1100dp.png)
- [Games_—_Russian_W390dp_H1400dp_ru](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/screenshot-tests/src/test/screenshots/jvm/com.alad1nks.oquturbo.feature.games.ui.GamesScreenKt.GamesScreenRussianPreview.Games_%E2%80%94_Russian_W390dp_H1400dp_ru.png)
- [Stats_—_rich_W390dp_H1600dp](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/screenshot-tests/src/test/screenshots/jvm/com.alad1nks.oquturbo.feature.stats.ui.StatsPreviewsKt.RichStatsPreview.Stats_%E2%80%94_rich_W390dp_H1600dp.png)
- [Number_Sprint_—_mistake_W390dp_H844dp_WITH_BACKGROUND](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/screenshot-tests/src/test/screenshots/jvm/com.alad1nks.oquturbo.feature.remembernumber.ui.RememberNumberScreenKt.RememberNumberScreenMistakePreview.Number_Sprint_%E2%80%94_mistake_W390dp_H844dp_WITH_BACKGROUND.png)
- [New_user_W390dp_H1000dp](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/screenshot-tests/src/test/screenshots/jvm/com.alad1nks.oquturbo.feature.profile.ui.ProfileScreenKt.ProfileNewUserPreview.New_user_W390dp_H1000dp.png)
- [Number_Sprint_menu_W390dp_H1000dp](https://github.com/alad1nks/oquturbo/blob/28fe2af66fc75f5edc7a204c55098996abd31770/screenshot-tests/src/test/screenshots/jvm/com.alad1nks.oquturbo.feature.remembernumbermenu.ui.RememberNumberMenuScreenKt.RememberNumberMenuScreenPreview.Number_Sprint_menu_W390dp_H1000dp.png)

## Журнал решений и передача

### Журнал D-001 • working

Опора — main 28fe2af, PRODUCT.md и текущие исходники + 6 PNG. Альтернатива: принять исходный аудит без сверки — отклонена из-за устаревшего названия и уже реализованных error/retry. Эффективность UX-решений не validated.

### Журнал D-002 • working

Приоритет трёх заданий: дневное занятие, свободная игра, понимание прогресса; настройки включены как поддерживающий путь. Альтернатива: начать с выбора палитры — отложена до задачи 04.

### Журнал D-003 • proposed

Явные кнопки результата вместо нажатия всей поверхности. Основание UX01-B. Альтернатива: оставить tap-anywhere с подписью — сохраняет риск случайного запуска. Поведение согласовать в 03/08, не менять механику зачёта.

### Журнал D-004 • working

Сохранить отдельный контекст плана и свободной игры, XP, серии рекордов, UTC и различие нуля/отсутствия данных. Альтернатива объединения прогресса отвергнута как изменение продуктовой семантики.

### Журнал D-005 • proposed

Сжать первый экран Главной и профиль новичка; информационные метки не выдавать за кнопки. Конкретная навигация и композиция не выбраны. Альтернатива сохранения текущей структуры остаётся для сравнения в задаче 02.

### Передача задаче 02

Вход: эти шесть листов, PRODUCT.md и docs/product/ux-redesign.md. Следующий шаг: inventory существующих функций → две структуры → рабочий выбор → старая функция/новое место → правила возврата и контекста. Сохранить ритм, обзор недели, серию/конфигурацию, настройки и все десять игр. Не приступать к детальным экранам раньше 03/04.

### Открыто и непроверено

Аудитория, бренд OquTurbo/Daily Mind, длительность, платформенный паритет, доступность. Runtime, сохранение/таймеры, back, screen reader и эффекты настроек не проверены. Мобильные стресс-проверки отложены до экранных задач. UI-компоненты и instances не создавались: задача 01 не проектирует интерфейс или дизайн-систему.

## Проверка результата

Созданы две страницы Figma с редактируемым текстом и Auto Layout. Проверены переносы текста и композиция; исправлена высота текстовых блоков. Продуктовые компоненты/instances не создавались: они относятся к задаче 05. Продуктовый код и исходные PNG не изменены. Kotlin/Gradle-тесты неприменимы к документационному изменению. Runtime-проверки не выполнены; timeout запуска не доказывает, что приложение невозможно запустить.
