# Задача 12 — реестр строк RU / EN / KK

Дата: 27 октября 2026. Связанная [спецификация и backlog](ux-handoff-12.md).
**Draft для реализации и языковой редактуры**, не утверждённый перевод. KK проверить носителем.
Таблица задаёт новые/изменённые смысловые строки; это не повтор всех существующих ресурсов.
Имена `ux_*` — предложенные ключи. Перед добавлением сверить эквивалентные текущие ключи:
один смысл не должен создавать дубликаты в разных features.

Пути будущих изменений:

- `resources/src/commonMain/composeResources/values/strings.xml` — EN;
- `resources/src/commonMain/composeResources/values-ru/strings.xml` — RU;
- `resources/src/commonMain/composeResources/values-kk/strings.xml` — KK;
- `resources/src/commonMain/kotlin/com/alad1nks/oquturbo/resources/AppResource.kt` — ручной facade.

`%1$d` — целое, `%1$s` — уже локализованное значение/название. Одинаковые placeholders
во всех локалях. Не склеивать предложение из отдельных переведённых слов. В строке цели
передавать локализованное количество очков через существующие/новые plurals.
Дата/время форматируются локально; UTC обозначается там, где это контракт статистики.
Макетные «Demo data», «граница прототипа», номера H/P/G/V и сценарные ошибки не продуктовые строки.

## Новые подписи и пояснения

| Ключ / задача | RU | EN | KK |
| --- | --- | --- | --- |
| ux_nav_today / 03 | Сегодня | Today | Бүгін |
| ux_nav_progress / 03 | Прогресс | Progress | Ілгерілеу |
| ux_profile_entry / 03 | Профиль и достижения | Profile and achievements | Профиль және жетістіктер |
| ux_home_show_plan / 04 | Показать план | Show plan | Жоспарды көрсету |
| ux_home_hide_plan / 04 | Скрыть план | Hide plan | Жоспарды жасыру |
| ux_home_next_exercise / 04 | Следующее упражнение | Next exercise | Келесі жаттығу |
| ux_goal_format / 04,08 | Цель: %1$s | Goal: %1$s | Мақсат: %1$s |
| ux_home_opening / 04 | Открываем упражнение… | Opening exercise… | Жаттығу ашылуда… |
| ux_home_later / 04 | Позже | Later | Кейін |
| ux_free_plan_note / 04,08 | Свободная игра не завершает дневной план | Free play does not complete the daily plan | Еркін ойын күнделікті жоспарды аяқтамайды |
| ux_catalog_play / 05 | К игре | Play | Ойнау |
| ux_example_label / 06,07 | Пример | Example | Мысал |
| ux_custom_example / 06 | Пример выбранного числа | Example with your settings | Таңдалған баптауларға сай мысал |
| ux_custom_empty / 06 | Выберите минимум две цифры | Select at least two digits | Кемінде екі цифрды таңдаңыз |
| ux_custom_one / 06 | Добавьте ещё хотя бы одну цифру | Add at least one more digit | Тағы кемінде бір цифр қосыңыз |
| ux_custom_decrease / 06 | Уменьшить длину числа | Decrease number length | Санның ұзындығын азайту |
| ux_custom_increase / 06 | Увеличить длину числа | Increase number length | Санның ұзындығын арттыру |
| ux_retry / 07,09,15 | Ещё раз | Try again | Қайта ойнау |
| ux_back_modes / 07 | К режимам | Back to modes | Режимдерге оралу |
| ux_back_game_settings / 07 | К настройкам игры | Back to game settings | Ойын баптауларына оралу |
| ux_comparison_title / 07 | Сравнение ответа | Answer comparison | Жауапты салыстыру |
| ux_digit_correct / 07 | Разряд %1$d · верно | Digit %1$d · correct | %1$d-цифр · дұрыс |
| ux_digit_incorrect / 07 | Разряд %1$d · ошибка | Digit %1$d · incorrect | %1$d-цифр · қате |
| ux_expected / 07 | Ожидалось | Expected | Күтілген жауап |
| ux_entered / 07 | Введено | Entered | Енгізілген жауап |
| ux_digit_groups / 07 | Разряды 1–5 сверху, 6–10 снизу | Digits 1–5 above, 6–10 below | 1–5-цифрлар жоғарыда, 6–10-цифрлар төменде |
| ux_goal_pending / 08 | Зачёт ещё не подтверждён | Completion is not confirmed yet | Орындалғаны әлі расталмады |
| ux_xp_pending / 08 | Ожидается +%1$d XP | +%1$d XP pending | +%1$d XP расталуы күтілуде |
| ux_next_exercise / 08 | Следующее упражнение | Next exercise | Келесі жаттығу |
| ux_back_plan / 08 | К плану | Back to plan | Жоспарға оралу |
| ux_grid_cell / 09 | Строка %1$d, столбец %2$d: %3$s | Row %1$d, column %2$d: %3$s | %1$d-жол, %2$d-баған: %3$s |
| ux_grid_selected / 09 | выбрана | selected | таңдалған |
| ux_grid_available / 09 | доступна | available | қолжетімді |
| ux_grid_shown / 09 | показана | shown | көрсетілген |
| ux_stats_available_journal / 11 | Попытки в доступном журнале | Attempts in the available history | Қолжетімді тарихтағы әрекеттер |
| ux_stats_totals / 11 | Накопленные итоги | All-time totals | Барлық уақыттағы қорытынды |
| ux_stats_unknown_date / 11 | Дата неизвестна | Date unknown | Күні белгісіз |
| ux_stats_unknown_settings / 11 | Настройки неизвестны | Settings unknown | Баптаулар белгісіз |
| ux_stats_chart_title / 11 | Очки по попыткам | Score by attempt | Әрекет бойынша ұпай |
| ux_stats_chart_attempt / 11 | Попытка | Attempt | Әрекет |
| ux_stats_chart_score / 11 | Очки | Score | Ұпай |
| ux_stats_chart_values / 11 | Значения графика | Chart values | График мәндері |
| ux_stats_no_data / 11 | Нет данных | No data | Деректер жоқ |
| ux_stats_not_recorded / 11 | Не записано | Not recorded | Тіркелмеген |
| ux_stats_insufficient / 11 | Недостаточно истории для сравнения | Not enough history to compare | Салыстыру үшін тарих жеткіліксіз |
| ux_stats_sample_count / 11 | Сохранено попыток: %1$d из 10 | Saved attempts: %1$d of 10 | Сақталған әрекеттер: %1$d / 10 |
| ux_stats_history_limited / 11 | История ограничена последними сохранёнными попытками | History is limited to the latest saved attempts | Тарихта тек соңғы сақталған әрекеттер бар |
| ux_stats_in_series / 11 | В серии: %1$s | In series: %1$s | Қатарда: %1$s |
| ux_stats_chart_point / 11 | %1$s · %2$s · %3$s | %1$s · %2$s · %3$s | %1$s · %2$s · %3$s |
| ux_profile_xp_explanation / 12 | Один правильный ответ — 1 XP. Новый уровень каждые 500 XP. | Each correct answer earns 1 XP. A new level every 500 XP. | Әр дұрыс жауапқа 1 XP беріледі. Әр 500 XP сайын жаңа деңгейге өтесіз. |
| ux_profile_no_unlocks / 12 | Пока нет новых открытий | No new unlocks yet | Әзірге жаңа ашылған мүмкіндіктер жоқ |
| ux_profile_date_unconfirmed / 12 | Дата получения не подтверждена | Date earned is not confirmed | Алынған күні расталмаған |
| ux_profile_frame_unavailable / 12 | Эта рамка пока недоступна | This frame is not available yet | Бұл жақтау әзірге қолжетімсіз |
| ux_profile_hidden_unavailable / 12 | Условие открытия пока недоступно | The unlock condition is not available yet | Ашу шарты әзірге қолжетімсіз |
| ux_settings_feedback / 13 | Звук и вибрация | Sound and vibration | Дыбыс және діріл |
| ux_settings_preference_only / 13 | Выбор сохраняется, но пока не влияет на игры | Your preference is saved, but does not affect games yet | Таңдауыңыз сақталады, бірақ әзірге ойындарға әсер етпейді |
| ux_theme_light / 13 | Светлая | Light | Ашық |
| ux_theme_dark / 13 | Тёмная | Dark | Күңгірт |
| ux_privacy_short / 13 | Приватность | Privacy | Құпиялық |

`ux_home_next_exercise` и `ux_next_exercise` обозначают один текст: при реализации использовать
один общий ключ, если контекст не требует разных форм. `ux_stats_unknown_settings` и
`ux_stats_sample_count` предпочтительно переиспользуют существующие home-ключи ниже.
`ux_stats_chart_point`: дата/время, локализованные очки, точное название серии; для screen reader
не заменяет отдельные оси и последовательный список всех значений. Grid-state выбирается
игрой; не озвучивать скрытые правильные клетки до показа.

Поправка 11 имеет приоритет: счётчик введённых цифр и номер попытки не добавляются
в активный HUD; длинная цепочка «увидел → запомнил → ввёл» не переносится в compact Ready V01.
Подпись диапазонов нужна только результату/сравнению, не активному полю.

## Переиспользовать или изменить существующее

Проверено по `values/strings.xml` на базе `324ba2f`; перед внедрением сверить три локали и facade.
Строки с тем же смыслом сохраняют существующие ключи. Таблица выше задаёт желаемую редакцию,
а не требование обязательно добавить `ux_*`.

| Существующий ключ / группа | Решение |
| --- | --- |
| home_start_training, home_continue_training, home_training_completed, home_training_item_completed, home_training_progress | Переиспользовать; динамические completed/total, не фиксировать три игры |
| home_training_congratulations_message | Изменить: убрать «все три игры»; RU «Вы выполнили дневной план», EN «You completed the daily plan», KK «Күнделікті жоспарды орындадыңыз» |
| home_training_score_goal | Для нового контекста использовать явную цель ux_goal_format + локализованные очки |
| home_training_loading/unavailable, home_retry_training, home_personal_result_loading/error/retry | Переиспользовать для соответствующего источника; не общий глобальный error |
| home_personal_result_count, home_personal_result_insufficient_hint, home_personal_result_unknown_settings | Переиспользовать в Stats при совпадении смысла; не создавать дубль без причины |
| home_personal_result_previous/current/median/window | Переиспользовать для 5+5 / 28 дней, не для других сравнений |
| remember_number_game_try_again | Изменить вопросительную подпись на действие ux_retry; не оставлять tap-anywhere |
| remember_number_game_start, remember_number_game_record/score/new_record | Переиспользовать; record всегда относится к точной серии |
| remember_number_game_correct_answer, remember_number_game_your_answer | Переиспользовать, если совпадает контекст; UX expected/entered нужен для таблицы сравнения |
| remember_number_menu_item_*_title/subtitle, *_digits_error | Существующие названия и правила; empty/one могут использовать общий error, если конкретная подсказка не нужна |
| stats_period_7_days/30_days/all_time, stats_weekday_*, stats_new_record_label, stats_open_details | Переиспользовать; All time не расширяет календарь до вымышленных дат |
| stats_training_not_completed, stats_period_empty_title/message | Переиспользовать; отсутствие активности не ошибка |
| remember_number_duration_less_than_one_second, *_duration_seconds/minutes/minutes_seconds | Переиспользовать формат длительности вместо округления положительного времени до нуля |
| profile_xp_to_next_format, profile_ranks_subtitle, profile_rank_range_format | Переиспользовать реальные остаток XP и диапазоны ранга |
| profile_achievement_memory_master_condition | Исправить смысл по mapper: RU «Рекорды в 3 разных режимах Числового спринта и Сетки памяти», EN «Records in 3 distinct Number Sprint and Memory Grid modes», KK «Сандар спринті мен Жад торының 3 түрлі режимінде рекорд орнатыңыз»; названия игр взять из текущих локализаций |
| profile_achievement_hidden_condition | Не обещать открытие от игры: использовать честный текст ux_profile_hidden_unavailable |
| profile_unlock_training_format для Explorer | Не использовать обещание открытия после 10 тренировок при isUnlocked=false всегда; ux_profile_frame_unavailable |
| profile_settings_language_*, profile_settings_system_default, profile_settings_theme, profile_save | Переиспользовать; System language не превращается в System theme |
| profile_settings_privacy | Короткий заголовок ux_privacy_short; текст-заглушка не становится действующей политикой |
| Названия 10 игр, режимов, навыков, ranks/items/achievements; reminder statuses/errors/actions | Переиспользовать существующие локализованные ресурсы. Новые тексты reminder не добавлять без расхождения контракта |

## Plurals и ошибки записи

Новые числовые фразы добавлять как plurals, а не конкатенацию. Минимальные формы для передачи:

| Смысл | RU one / few / many / other | EN one / other | KK other |
| --- | --- | --- | --- |
| Попытки | %1$d попытка / %1$d попытки / %1$d попыток / %1$d попытки | %1$d attempt / %1$d attempts | %1$d әрекет |
| Очки | %1$d очко / %1$d очка / %1$d очков / %1$d очка | %1$d point / %1$d points | %1$d ұпай |
| Правильные ответы | %1$d правильный ответ / %1$d правильных ответа / %1$d правильных ответов / %1$d правильного ответа | %1$d correct answer / %1$d correct answers | %1$d дұрыс жауап |
| Клетки | %1$d клетка / %1$d клетки / %1$d клеток / %1$d клетки | %1$d cell / %1$d cells | %1$d ұяшық |

Значения целочисленные; окончательные категории и fallback сверить с Compose Resources.
Существующие plurals переиспользовать, когда они выражают тот же смысл (например stats_correct_answers_count).
Проверить 0, 1, 2, 5, 11, 21 и перенос длинной составной цели.

Тексты failure/retry записи **не финализированы** до H12-00: «не сохранено», «сохранено частично»
и «статус неизвестен» нельзя выбирать без факта. После контракта добавить отдельные ключи
для установленных исходов RU/EN/KK. Не переносить ложный успешный timeout из прототипа.

## Приёмка локализации

H12-05/06/07/11/12/13/14 включают строки своего экрана в один PR с UI и AppResource.
H12-16 проверяет полный EN/KK каталог, самые длинные режимы при 150%, вложенные Theme/Reminders,
ошибки и составные значения. Проверка присутствия ключей не доказывает отсутствие обрезки.
Ни draft-таблица, ни отдельный EN/KK specimen не снимают V-07 без языковой и визуальной проверки.
