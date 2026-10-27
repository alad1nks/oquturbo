# Задача 08 — игровой цикл и результат

## Статус и база

08 · Игровой цикл и результат · Ready по критериям дизайн-задачи. 25.10.2026 · main cccc6e4 · ветка ux-play-result. Визуальное направление 06/07 v2 остаётся working, не одобрено пользователем. Продуктовый код не менялся.

## Создано

09_Play_Result: 12 основных Number Sprint frames; 40 поддерживающих экранов Binary и Custom 02 длиной 2–10; 5 дополнительных шагов свободного/дневного сценария; 3 основных Memory Grid состояния и 4 шага последовательности; 6 stress-экранов, 2 full-scroll specimens, сценарный стенд. Все числа демонстрационные. Шесть игровых семейств на 06_Components: Context, Metrics, NumberStage, Comparison, Goal, GridCell. Пять семейств имеют TextScale 1/1.5; GridCell — пять содержательных состояний. Переиспользованы Button и Foundations.

## Поведение и данные

Number Sprint: 4 цифры в Classic/Binary, длина не растёт. Автопроверка после полной длины; верно → +1 очко и сразу следующее число. Первый показ 1700 мс, далее базовые 1000 × 0,95 после каждого верного ответа с целочисленным округлением. Отдельной остановки «Верно», кнопки проверки или паузы нет. Ошибка завершает попытку. Цель 5 не останавливает игру. isTrainingResultReady блокирует продолжение до подтверждения; pending не имеет таймера успеха. Новый рекорд показывается только после подтверждения repository. Free play не завершает план.

## Прототип и границы

P01 → P02 → P03: в P03 уже введено 482; 1 завершает с 0 очков, 7 даёт 1 и новое 9052 → поддерживающий ввод → ошибка с 1 очком. Дневной P08 → показ → ввод → ошибка с 0 → повтор. P09 (4), P10 (pending 6), P11 (зачёт 6), P12 (последний пункт другого плана) — отдельные fixtures через сценарный стенд, не искусственные скачки счёта. Клавиатура показывает только последние цифры сценария; свободный ввод, удаление, paste, IME и произвольная последовательность не эмулируются. Memory Grid: маршрут 0→4→8, показ по 700 мс, первая клетка верна, затем ошибка; итог 0 очков / +1 XP. Произвольные нажатия не моделируются.

## Связи и возвраты

Каталог Classic/Binary и Custom 02 каждой длины ведут в точный Ready через URL-мосты. Выход Custom возвращает в настройки той же длины, повтор сохраняет серию. H17 из Главной открывает дневной Ready 08. P11 возвращает к частичному плану H02, P12 — к завершённому H03; следующее упражнение ведёт на существующий стенд Wide Eye H18. Memory Grid доступен через явный Route-вход границы каталога, другие режимы не заменены. Межстраничные URL-мосты не моделируют runtime back stack.

## Коррекция визуала Number Sprint · 27 октября

По замечанию пользователя цифры снова показаны в отдельных ячейках во всех NumberStage:
Ready, показ, ввод, Custom/Binary и stress-результаты. Пустая позиция имеет каретку,
поле центрировано. Для 6–10 цифр — две сбалансированные строки, а не текст с переносом
после пятого символа; для 10 цифр сохраняется 5+5. P06 теперь сравнивает полный ответ
и правильное число двумя одинаковыми сетками с текстом ошибки.
Детали компонентов, границы совместимости и проверки —
[доработка задачи 11](ux-validation-11.md#доработка-number-sprint--27-октября).
Это актуальное визуальное уточнение D-038; игровые правила и ограничения сценариев прежние.

## D-037–D-040

D-037 · working: общая оболочка и метрики, игровые поля и счётчики остаются специфичными; альтернатива универсального игрового поля отвергнута из-за разных правил.
D-038 · working: сравнение по разрядам с текстом «верно/ошибка», длинный ответ группами 1–5 / 6–10; цвет не единственный сигнал. Альтернатива мелкого однострочного числа ухудшает чтение.
D-039 · working: подтверждённый зачёт определяет действие и возврат; pending не обещает XP. Альтернатива оптимистического успеха создаёт ложное выполнение.
D-040 · working: Noto Sans, игровые поверхности и радиусы 06/07 v2; декоративные обложки не становятся настоящими полями. D-003 и D-012 сохраняются: явные кнопки, повтор через Ready — изменение поведения. D-014 остаётся proposed: ошибки сохранения не имеют готового retry API.

## Применимость остальных игр

Wide Eye / kenkozgame: Context, Metrics, Goal, Button; сохранить периферические стимулы и режимы, не вводить паузу Number Sprint.
Don’t Tap / baspagame: Context, Metrics, Goal, Button; сохранить togglePause и таймер реакции. Правильное ненажатие увеличивает correctAnswers без увеличения счёта: XP ≠ score.
Word Flow: оболочка и результат; свои слова, отвлекающие варианты, таймер и pause/resume.
Dual Focus: оболочка и результат; две задачи и своя пауза, не переносить сравнение цифр.
Rotation Match: оболочка и результат; геометрические стимулы и pause/resume.
Number Trail: оболочка и результат; последовательность чисел, свой elapsed timer и pause/resume.
Symbol Count: оболочка и результат; набор фигур и варианты ответа, своя пауза.
Rule Switch: оболочка и результат; активное правило и стимулы, своя пауза.
Новые поля для этих игр не нарисованы. Дневной Goal применим только к Number Sprint, Wide Eye и Don’t Tap.

## Проверки и ограничения

Структура страницы до handoff: 352 instances, 756 text nodes, 238 frames; только Noto Sans; нет UI image fills, missing local destinations, click-зон <48×48 или текста шире родителя. 79 локальных переходов плюс URL-мосты. Визуально просмотрены Ready, результат, сравнение, Memory Grid, RU/KK 320×640 150% с полным содержимым, EN/KK, dark, 800×1100. Compact scroll: 1070/1064 px при viewport 532. Wide колонка 560 px. Исправлены высоты строк Memory Grid. Контраст существующих ролей основан на 05, палитра не расширена.
Это статические renders и проверка сохранённого графа, не ручной walkthrough Presentation viewer и не пользовательский тест. Таймеры, IME, screen reader, системный back, focus, фон приложения, сохранение и восстановление записи на устройствах не проверены. EN/KK — draft, без языковой редактуры. Gradle/Roborazzi не запускались: продуктовый код не изменён.

## Передача 09–12

09: различать score / correctAnswers / XP, записи журнала и all-time aggregates, точные нормализованные серии и pending. Custom 4/0123456789 = Classic, 4/01 = Binary, остальные length:N;digits:S. 11: проверить viewer, доступность, реальные клавиатуры/таймеры/возвраты, 150% и понимание сравнения. 12: реализовать явные действия, новый Ready при повторе, локализованные строки RU/EN/KK и AppResource; не обещать retry записи без контракта идемпотентности. Repository handoff: docs/product/ux-play-result-08.md.

## Прямые ссылки

- [09_Play_Result](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1675).
- [Прототип со сценариями](https://www.figma.com/proto/kElntn3nRWIQuFjyxUJNBM?node-id=89-4322&starting-point-node-id=89%3A4322).
- [00_Project: handoff](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=90-703).
- [Галерея компонентов](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-4582).

| Frame | Состояние |
| --- | --- |
| [P01](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1717) | Ready — свободная игра |
| [P02](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1718) | Показ числа |
| [P03](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1719) | Ввод |
| [P04](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1720) | Верно — следующее число |
| [P05](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1721) | Ошибка и итог |
| [P06](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1722) | Сравнение разрядов |
| [P07](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1723) | Новый рекорд |
| [P08](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1724) | Ready — дневное задание |
| [P09](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1725) | Цель не достигнута |
| [P10](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1726) | Зачёт ожидается |
| [P11](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1727) | Цель достигнута |
| [P12](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1728) | Последнее задание |

| Memory Grid | Состояние |
| --- | --- |
| [M01](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-1415) | Ready |
| [M02](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-1416) | Поле — показ 1 |
| [M02b](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-1417) | Показ 2 |
| [M02c](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-1418) | Показ 3 |
| [M02d](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-1419) | Ввод |
| [M02e](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-1420) | Ввод после верной клетки |
| [M03](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-1421) | Результат |

| Серия | Ready | Показ | Ввод | Результат |
| --- | --- | --- | --- | --- |
| Binary | [Ready](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-141) | [Показ](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-142) | [Ввод](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-143) | [Результат](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-144) |
| Custom 2 / 02 | [Ready](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-218) | [Показ](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-219) | [Ввод](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-220) | [Результат](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-221) |
| Custom 3 / 02 | [Ready](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-295) | [Показ](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-296) | [Ввод](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-297) | [Результат](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-298) |
| Custom 4 / 02 | [Ready](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-372) | [Показ](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-373) | [Ввод](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-374) | [Результат](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-375) |
| Custom 5 / 02 | [Ready](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-449) | [Показ](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-450) | [Ввод](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-451) | [Результат](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-452) |
| Custom 6 / 02 | [Ready](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-526) | [Показ](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-527) | [Ввод](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-528) | [Результат](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-529) |
| Custom 7 / 02 | [Ready](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-603) | [Показ](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-604) | [Ввод](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-605) | [Результат](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-606) |
| Custom 8 / 02 | [Ready](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-680) | [Показ](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-681) | [Ввод](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-682) | [Результат](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-683) |
| Custom 9 / 02 | [Ready](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-757) | [Показ](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-758) | [Ввод](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-759) | [Результат](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-760) |
| Custom 10 / 02 | [Ready](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-834) | [Показ](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-835) | [Ввод](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-836) | [Результат](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-837) |

| Stress | Ссылка |
| --- | --- |
| S01 · RU 320 / 150% | [Frame](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-4350) |
| S02 · KK 320 / 150% | [Frame](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-4374) |
| S03 · EN | [Frame](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-4398) |
| S04 · KK | [Frame](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-4422) |
| S05 · Dark | [Frame](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-4446) |
| S06 · Wide | [Frame](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-4470) |

## Контракт реализации и источники

Вход: [план](ux-redesign.md), [05](ux-foundations-05.md),
[06 v2](ux-home-training-06.md), [07 v2](ux-catalog-07.md),
[wireframes 03](ux-wireframes-03.md), [PRODUCT.md](../../PRODUCT.md).
Визуальная подача развивает 06/07 v2; их статус Draft для оценки пользователя не отменён.

Проверены исходники на `cccc6e4`:

- `feature/remembernumber/.../ui/RememberNumberViewModel.kt`, `RememberNumberUiState.kt`,
  `RememberNumberScreen.kt`: автоматический ввод, таймер, запись, training gate, выход.
- `feature/memorygrid/.../model/MemoryGridGame.kt` и `ui/MemoryGridViewModel.kt`:
  Route/Reverse/Flash, клеточный ввод, score/correctCellCount, запись и таймеры.
- ViewModel/model модулей `kenkozgame`, `baspagame`, `wordflow`, `dualfocus`,
  `rotationmatch`, `numbertrail`, `symbolcount`, `ruleswitch`: карта исключений выше.
- `core/ui/.../component/GameStateOverlay.kt`, `GameResultCard.kt`,
  `GameHeader.kt`, `GameStagePanel.kt`: точки будущей интеграции.

| Figma | Будущая реализация | Контракт |
| --- | --- | --- |
| [Play/Context](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1684) | modify GameHeader + route context | Игра, нормализованная серия, источник запуска; без корневой панели внутри игры |
| [Play/Metrics](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1693) | modify GameResultCard / GameScoreBadge | Очки, рекорд серии и XP раздельно; правильные ответы передаются игрой, не вычисляются из score |
| [Play/NumberStage](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1702) | modify RememberNumberTextField + GameStagePanel | Полная длина без ellipsis; до 5 разрядов в строке; подсказка диапазонов при переносе |
| [Play/Comparison](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1709) | modify answer comparison | Номер позиции, ожидаемое, введённое, слово «верно/ошибка»; доступный последовательный порядок чтения |
| [Play/Goal](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=87-1716) | modify training result content | Цель, факт завершения и подтверждённый прогресс; не интерактивен |
| [Play/GridCell](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=89-1414) | modify MemoryGridScreen cells | Поле и порядок принадлежат режиму; accessible label — строка/столбец/состояние |
| Button из 05 | modify GameStateOverlay / AnimatedGameStateOverlay | Явное действие; фон без onClick; disabled не реагирует |

Exit в активной попытке вызывает текущий back callback без новой паузы или подтверждающего
диалога. Незавершённая попытка не записывается как завершённая. Дизайн не обещает её восстановить.
Для Number Sprint / Memory Grid нет новой кнопки Pause. Поведение при уходе приложения в фон
требует платформенной проверки; Figma его не моделирует.

Memory Grid: Route — повтор по порядку, Reverse — обратный порядок, Flash — все выделенные
клетки в любом порядке без повторов. Подробный макет относится к Route.
Первые три клетки показываются по 700 мс; далее длительность уменьшается на 35 мс
за добавленную клетку до 350 мс. Успешный раунд имеет feedback 500 мс.
Route/Reverse: 3×3 до длины 6, 4×4 до 10, затем 5×5; повтор клетки возможен с длины 7.
Flash: 4×4, длина до 16. Игры в дневном плане для Memory Grid нет.

Новые подписи в Figma — проект строк для будущих RU/EN/KK ресурсов, не изменения AppResource:
«Сравнение ответа», «Разряд N · ошибка/верно», «Ожидалось / введено»,
«Разряды 1–5 сверху, 6–10 снизу», «Зачёт ещё не подтверждён»,
«Ожидается +N XP», «Свободная игра не завершает дневной план».
При реализации применять plural resources для очков, ответов и клеток.
EN/KK stress-подписи являются draft и требуют языковой редактуры.

## Проверка и открытые вопросы

Проверены структура слоёв, свойства компонентов, локальные destinations, размеры click-зон,
шрифты и статические изображения. Полный набор UI не заменён растрами.
Неактивный фон результата и кнопка pending без реакции проверены по графу.
Прямые переходы между страницами сохранены как URL-мосты; тест browser/viewer не проводился.

После доработки 27 октября в Figma показана полная цифровая клавиатура 0–9 и удаление.
Это иллюстрация системного IME; работают только прежние сценарные реакции последних цифр.
Произвольный ввод/удаление не эмулируются, это не ограничение допустимого ввода приложения.
Все допустимые цифры, удаление, paste и аппаратная клавиатура остаются контрактом существующего поля.
Для остальных игр карта применимости не заменяет проектирование их собственных полей.

D-014 остаётся открытым: нет состояния ошибки сохранения и безопасного retry API у Number Sprint.
Не добавлен ложный успешный timeout или повторная запись. Если хранение не отвечает, доступен
выход к плану; стратегия восстановления требует отдельного контракта и тестов.
Шрифты/лицензии на targets, реальные таймеры и доступность остаются непроверенными.

Gradle, unit tests и Roborazzi не запускались: изменяются только дизайн и документация.
Перед завершением выполнены `git diff --check`, просмотр diff и status.
