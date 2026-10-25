# Задача 07 — игровой каталог, режимы и настройки

Дата: 25 октября 2026. **Версия 2 · Draft для визуальной оценки**.
Структурные и визуальные проверки выполнены; соответствие игровому характеру ещё не подтверждено пользователем.
Первая версия получила замечание: интерфейс выглядит как учёт привычек и плохо подходит играм.
По прямому поручению пользователя переработаны композиция и визуальная подача, а не только цвета.

База: `main` / `4da39b0473f3c183e6dbc970c1b0a4a6a201db7b`; ветка `ux-catalog`, PR #155.
Результат v1 сохранён в Git и на Figma canvas как архив. Продуктовый код и механики не менялись.
Вход: [план](ux-redesign.md), [архитектура 02](ux-architecture-02.md),
[система 05](ux-foundations-05.md), [Главная 06](ux-home-training-06.md), [PRODUCT.md](../../PRODUCT.md).

## Результат

[08_Catalog](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=56-17) ·
[Прототип](https://www.figma.com/proto/kElntn3nRWIQuFjyxUJNBM?node-id=56-18&starting-point-node-id=56%3A18) ·
[00_Project: handoff v2](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=61-17) ·
[Архив v1](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=69-666).

- Вместо одинаковых текстовых карточек — две колонки игр с десятью различимыми векторными мотивами:
  числа, поле зрения, подходящий/неподходящий сигнал, клетки, чтение, две задачи, вращение,
  маршрут, подсчёт фигур и смена правила. Это декоративные иллюстрации механик, не игровые поля.
- На 390×844 четыре первые игры полностью видны. На 320/150% — одна колонка и вертикальная прокрутка.
- Заголовки без засечек, короткие описания, действие внутри карточки; длинное вступление убрано.
  Информационные навыки остаются обычным текстом. Поиска и фильтров нет.
- Режимы различаются примерами `4827`, `0101`, `2020`; подпись явно обозначает их как примеры.
- Custom показывает пример выбранного числа. Вместо slider предложен ограниченный stepper `− / N / +`.
  Диапазон 2–10 и минимальные две цифры сохранены. Это изменение UI-контрола, не правил игры.

Семь основных frames сохранили IDs, поэтому прежние ссылки работают:

| Frame | Содержание |
| --- | --- |
| [C01: каталог](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=56-18) | Все десять игр, рисунки механик, действия и навыки |
| [C02: режимы](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=56-19) | Три компактных режима с числовыми примерами |
| [C03: свой набор](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=56-20) | Возвращающийся пользователь, длина 4, цифры 0/2, пример `2020` |
| [C04: одна цифра](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=56-21) | Выбран 0; пример скрыт, ошибка рядом с выбором, CTA недоступен |
| [C05: пустой набор](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=56-22) | Пустой выбор, конкретная подсказка восстановления |
| [C06: передача серии](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=56-23) | Граница задачи 08 с точной конфигурацией 4 / 02 и возвратом |
| [C07: первый вход](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=56-24) | Реальные defaults: длина 4, все цифры, серия Classic |

## Свидетельства текущего поведения

Проверены `feature/games/.../ui/GamesUiState.kt`, `model/TrainingGame.kt`,
`feature/remembernumbermenu/.../ui/RememberNumberMenuCustomOptionsDialog.kt`,
`RememberNumberMenuUiState.kt`, `RememberNumberMenuViewModel.kt`,
`feature/remembernumber/.../ui/RememberNumberViewModel.kt` и RU/EN/KK ресурсы.
Это чтение кода, не запуск приложения.

- Каталог: Number Sprint — 3 режима; Wide Eye — 4; Don't Tap — 7; Memory Grid — 3.
  Word Flow, Dual Focus, Rotation Match, Number Trail, Symbol Count, Rule Switch — по одному,
  с прямым переходом в Ready. Новых игр и меню нет.
- Custom defaults: длина 4, все десять цифр. Slider ограничивает длину 2–10, шаг 1.
  Реальная UI-валидация выбора: `count { it } >= 2`; ноль и одна цифра недопустимы.
  Ввод произвольного числа, ошибки формата и сетевые ошибки для этой формы не добавлены.
- Закрытие формы меняет только `show`; выбранные значения сохраняются в текущем ViewModel.
  Это не обещание восстановления после завершения процесса.
- `sessionMode()` нормализует набор цифр и определяет серию по конфигурации:
  `4 + 0123456789 → Classic`, `4 + 01 → Binary`, иначе
  `Custom / length:N;digits:S`. Поэтому «4 / 01» нельзя показывать отдельной Custom-серией.
  Демонстрационная независимая Custom-серия — «4 / 02».
- Свободный запуск не завершает пункт дневного плана; storage keys, IDs и маршруты не меняются.

## Компоненты и визуальная система

[Галерея Default/Pressed/Focus и крупного текста](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=59-151).

| Компонент | Контракт / карта реализации |
| --- | --- |
| [GameItem/Playful](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=68-284) | Default/Pressed/Focus × TextScale 1/1.5; название, описание, навыки, действие, Artwork INSTANCE_SWAP. modify GamesScreen |
| [ModeItem/Playful](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=68-321) | 6 вариантов; пример рядом с описанием на обычной ширине, вертикальная композиция при 150%. modify GameMenuItem |
| [Catalog/Example](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=68-338) | Valid/Invalid × TextScale 1/1.5; Label, Value, Hint; только пример, не таймер и не текущий стимул |
| Catalog/Art/* | 10 отдельных vector masters, сменяемых через Artwork; пропорции рисунка сохраняются на широкой карточке |
| Button, SelectableChip, InlineFeedback, Catalog/Navigation | Переиспользование существующих действий, выбора, локальной ошибки и навигации |

В существующую коллекцию Foundations добавлены `catalog/bg`, пять акцентов иллюстраций,
`catalog/art-ink`, радиусы карточки 20 и контрола 12. Семантические цвета ссылаются на primitives;
UI-состояния используют прежние action/error/focus/disabled. Основной текст и все активные экраны — Noto Sans.
Новые text styles: catalog-title 28/36, catalog-card 18/24, catalog-number 36/44 и их 150% варианты.

Это локальное развитие направления по замечанию пользователя. Другие страницы 04–06 автоматически
не переведены на новую типографику и композицию; глобальную согласованность проверить перед финальным handoff.
Компоненты v1 сохранены для архивных экземпляров. Нативный UI, новые строки RU/EN/KK и AppResource — отдельная реализация.

## Поведение и прототип

Вся карточка игры — одно действие, без вложенных кнопок. Число режимов показывается, когда есть выбор;
игры с прямым входом подписаны «К игре». Навыки не фильтруют каталог.
В Custom галочка и фон обозначают выбор, ошибка имеет текст, недопустимый набор не запускается.
На 320/150% цифры идут в три колонки. Возврат к режимам заменяет отдельное повторяющееся действие «Отмена»;
по контракту значения сохраняются в текущем ViewModel, сессия не запускается.

Основной сценарий: C01 → C02 → C03 → C06. C03 представляет повторное открытие настроек 4 / 02.
Первый вход C07 доступен отдельным flow start и передаёт конфигурацию в Classic.
C03: убрать 2 → C04; C04: добавить 2 → C03, убрать 0 → C05; C05: добавить 0 → C04.

Для выбранного набора 02 добавлены восемь поддерживающих состояний длины и восемь границ Ready:

| Длина | Настройки | Передача точной серии |
| --- | --- | --- |
| 2 | [L2](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-645) | [R2](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-695) |
| 3 | [L3](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-714) | [R3](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-764) |
| 4 | C03 | C06 |
| 5 | [L5](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-783) | [R5](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-833) |
| 6 | [L6](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-852) | [R6](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-902) |
| 7 | [L7](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-921) | [R7](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-971) |
| 8 | [L8](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-990) | [R8](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-1040) |
| 9 | [L9](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-1059) | [R9](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-1109) |
| 10 | [L10](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-1128) | [R10](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=71-1178) |

Step `−/+` меняет длину на 1, пример и подпись серии; на 2/10 соответствующее действие disabled.
Числа демонстрационные, не случайный текущий стимул. Прототип покрывает этот сценарий, а не произвольные
1024 комбинации цифр. Для других наборов и локализованных stress-экранов stepper показан как specimen;
нет реакции не означает продуктовый запрет. UI-реализация должна поддерживать весь допустимый выбор.

Все остальные игры сохраняют явно подписанные обратимые границы существующих маршрутов.
Игровые поля и настоящий Ready относятся к задаче 08. Из H13 задачи 06 открывается каталог;
Сегодня/Прогресс/Настройки ведут в прежние материалы через URL-мосты, не единый runtime back stack.

## Проверки v2

| Проверка | Свидетельство |
| --- | --- |
| 390×844 | Каталог: viewport 622, content 1420; четыре первые игры видны полностью. Custom C03/C07: content 636 при viewport 686, CTA виден |
| [Каталог RU 320/150%](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=58-263) | Одна колонка, content 3176 / viewport 244; stacked navigation вне scroll |
| [Каталог KK 320/150%](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=58-338) | Все десять игр и длинные названия; content 3272 / viewport 244 |
| [Custom RU 320/150%](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=58-413) | Content 1156 / viewport 478; все тексты увеличены, ошибка и действие доступны прокруткой |
| [EN](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=58-461), [KK](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=58-509) | Проверены переносы и полная локализация error-формы; новые подписи — draft для языковой редактуры |
| [Dark](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=58-557) | Пример, selection, error и disabled читаемы; цвет не единственный сигнал |
| [800×1100](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=58-605) | Колонка ограничена 680, рисунки сохраняют пропорции |
| Границы длины | Просмотрены 2 и 10; действия выхода за диапазон отключены, серия передаётся с нужной длиной |
| Компоненты | Просмотрены Default/Pressed/Focus и ModeItem 150%; все новые текстовые узлы используют text styles |
| Структура текущих frames | 445 instances, 779 text nodes, 265 vector nodes; Noto Sans; UI image fills отсутствуют; архив исключён |
| Граф | 152 сохранённые реакции; отсутствующих локальных destinations и click-зон меньше 48×48 нет |

Full-scroll specimens: [форма RU](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=59-629),
[каталог KK](https://www.figma.com/design/kElntn3nRWIQuFjyxUJNBM?node-id=59-656).
Это служебные листы полного контента, не замена viewport-экранам.

Проверены структура, сохранённый граф и статические renders. Presentation viewer вручную,
реальные жесты, screen reader, keyboard focus, runtime-сохранение и участники не проверены.
Контраст прежних UI-ролей основан на 05; новые иллюстрации декоративные, их цвет не кодирует обязательные данные.
Gradle/Roborazzi не запускались: diff только документационный.

## Решения и передача

| ID | Статус | Решение |
| --- | --- | --- |
| D-029 | superseded | Текстовые карточки v1 отвергнуты после замечания пользователя о неигровом характере |
| D-030 | working | Отдельный прокручиваемый Custom-экран; диапазоны, валидация и сохранение выбора в текущем стеке прежние |
| D-031 | working | Серия определяется конфигурацией, включая совпадения Classic/Binary; storage не меняется |
| D-032 | working | Игровые маршруты — обратимые границы; 08 создаёт полный цикл, межстраничные URL-мосты обозначены |
| D-033 | working | Две колонки с рисунками механик, одна колонка при 150%, Noto Sans и короткие подписи. Основание — прямое замечание пользователя; новая версия ещё не одобрена |
| D-034 | proposed | Stepper вместо slider и обновляемый пример. Меньше абстрактной настройки; требует проверки удобства длины 2–10, клавиатуры и screen reader |

Задаче 08: использовать переданную длину/цифры и свободный контекст, заменить границы на Ready;
не записывать попытку до завершения и не засчитывать дневной план. Не переносить иллюстрации каталога
как реальные поля других игр. Задаче 09: сравнивать точные нормализованные серии.
Задаче 11: проверить обнаруживаемость игр, весь выбор цифр, stepper, возврат и сохранение позиции;
согласовать обновлённую игровую подачу с оставшимися экранами. Задаче 12: подготовить строки RU/EN/KK
и AppResource при реализации. Новые ресурсы, механики и правила хранения в этом PR не меняются.
