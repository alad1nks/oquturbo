# План внедрения screenshot-тестов

## Цель

Добавить визуальные regression-тесты для Compose Multiplatform UI, которые:

- запускаются без эмулятора на GitHub Actions;
- проверяют общий `commonMain` UI, используемый OquTurbo и standalone-продуктами;
- дают воспроизводимый diff и HTML-отчёт при изменении изображения;
- позволяют осознанно обновлять и ревьюить эталонные PNG (goldens);
- не требуют делать внутренние screen composable публичными ради тестов.

## Текущее состояние проекта

Факты, подтверждённые репозиторием:

- В `settings.gradle.kts` подключены 34 модуля: четыре продукта (`oquturbo`, `kenkoz`, `baspa`, `sansprint`), общие feature/core/resource-модули и launcher-модули для Android, Desktop и Web.
- UI feature-модулей находится в `commonMain`; каждый UI-модуль уже имеет JVM target и Android host-test configuration.
- В 14 Kotlin-файлах есть 36 `@Preview`. Они уже покрывают Home, Games, Stats, Profile, меню и состояния трёх игр, включая dark, tablet, dialog и error-сценарии.
- Большинство leaf screen composable и preview-функций имеют `private`/`internal` visibility. Это хорошо для production API, но затрудняет создание внешнего Android test harness без рефакторинга видимости.
- Тема централизована в `core:designsystem`; существующие previews используют `OquTurboTheme`. Строки приходят из Compose Resources через `AppResource` и имеют `values`, `values-ru`, `values-kk`.
- Gradle wrapper — 9.6.1, AGP — 9.3.0, Kotlin — 2.4.10, Compose Multiplatform — 1.11.1. Workflow устанавливают JDK 17, но `gradle/gradle-daemon-jvm.properties` закрепляет Gradle daemon toolchain 21; JVM-варианты KMP-модулей поэтому загружаются screenshot runtime на Java 21.
- В PR workflow уже выполняется `./gradlew test`, но тестовые source set практически пусты; визуальных проверок и сохранённых эталонов нет.
- GitHub Actions сейчас дублирует `./gradlew test` в продуктовых workflow. Screenshot-проверку лучше вынести в отдельный job/workflow, а не размножать по четырём продуктам, потому что feature UI общий.

## Выбор подхода

| Вариант | Плюсы | Ограничения для этого проекта | Решение |
| --- | --- | --- | --- |
| Roborazzi + Compose Desktop preview scanner | Headless JVM-запуск, быстро, умеет сканировать Compose Multiplatform `@Preview` из `commonMain`, включая private previews; есть record/verify/compare и HTML report | Desktop/Skia pixels не равны Android pixels; функция preview scanner пока experimental; scanner требует минимум JVM 17, а этот репозиторий фактически собирает JVM-варианты на toolchain 21 | **Основной подход** |
| Roborazzi + Robolectric | Android-like rendering без эмулятора | Для используемого `com.android.kotlin.multiplatform.library` известна проблема: Roborazzi-задачи для `androidHostTest` не создаются; потребовалась бы отдельная Android-обвязка и изменение visibility/fixtures | Не использовать как основу |
| Official Compose Preview Screenshot Testing | Официальный Layoutlib renderer, `validateDebugScreenshotTest`, хороший отчёт | Инструмент Android-only и alpha; тестовые `@PreviewTest` должны жить в Android `screenshotTest`, поэтому существующие private KMP previews напрямую не переиспользуются | Вернуться к оценке после появления полноценной KMP-поддержки |
| Instrumented screenshots на emulator | Максимальная Android fidelity | Медленнее, дороже и более flaky в CI; не проверяет Desktop/Web и избыточен для shared leaf UI | Только отдельным будущим слоем для 2–3 критичных end-to-end экранов |

Рекомендуемая архитектура: отдельный test-only JVM-модуль `:screenshot-tests`, который зависит от JVM-вариантов UI-модулей и через Roborazzi Compose Desktop Preview Scanner находит previews в runtime classpath. Сам модуль компилируется под JVM 21 в соответствии с уже закреплённым Gradle daemon toolchain, поэтому production JVM targets менять не нужно. Goldens хранятся в этом модуле и проверяются одной Gradle-командой.

Такой тест проверяет shared Compose layout, тему, ресурсы и состояния. Он намеренно не обещает pixel fidelity Android status/navigation bars или OEM rendering.

## Область работ

Входит:

- shared design primitives и leaf UI;
- существующие previews и недостающие детерминированные preview-сценарии;
- light/dark, compact/phone/tablet и точечное покрытие всех трёх локалей;
- record/verify workflow, отчёты и процесс ревью goldens;
- запуск на pull request в GitHub Actions.

Не входит:

- snapshot полного приложения с реальным Koin, навигацией и DataStore;
- Android/iOS/Web pixel-perfect parity;
- автоматическое принятие и коммит goldens ботом;
- проверка поведения, accessibility и navigation back stack — для них нужны обычные UI/unit/integration tests;
- изменение application ID, package namespace, platform targets, storage или navigation arguments.

## Этап 0. Compatibility spike

Сначала подключить минимальный прототип, не размечая все previews.

1. Создать `screenshot-tests/build.gradle.kts` и временно подключить только `core:ui` и один resource-heavy feature, например `feature:stats`.
2. В `gradle/libs.versions.toml` зафиксировать одну совместимую версию Roborazzi и matching-версию ComposablePreviewScanner. Версию выбрать по результату dependency resolution с текущими Gradle 9.6.1, Kotlin 2.4.10 и Compose 1.11.1; не использовать dynamic versions.
3. Применить `org.jetbrains.kotlin.jvm`, Compose Multiplatform, Compose Compiler и Roborazzi plugins только к `:screenshot-tests`; выставить JVM toolchain/target 21 только в этом test-only модуле, чтобы runtime мог загрузить JVM-варианты KMP dependencies.
4. Добавить `roborazzi-compose-desktop-preview-scanner-support`, ComposablePreviewScanner и JUnit 4 в test configuration, а Skiko runtime — в runtime/test runtime по фактической конфигурации библиотеки.
5. Ограничить scanner package prefix значением `com.alad1nks.oquturbo`, включить private previews и проверить, что scanner видит previews в project dependencies, а не только в собственном source set.
6. Проверить рендер `OquTurboTheme`, Material icons и Compose Resources на Linux; отдельно убедиться, что кириллица и казахские символы не заменяются tofu glyphs.
7. Локально получить задачи `recordRoborazziJvm`, `verifyRoborazziJvm`, `compareRoborazziJvm`, затем повторить record и verify на GitHub-hosted `ubuntu-24.04` с JDK 21.
8. Проверить, что configuration cache не ломает generated tests и что повторный verify без изменений даёт нулевой diff.

Критерий выхода из spike: два последовательных CI-run на одном commit создают идентичные PNG, private previews из зависимых KMP-модулей найдены, resources загружены, отчёт и diff доступны как artifact.

Если scanner не видит previews из project dependencies, fallback — применить Roborazzi только к JVM test source sets выбранных feature-модулей. Production visibility не расширять; preview-сценарии остаются в своих модулях. Robolectric/Android host tests не использовать до подтверждённого исправления совместимости с Android-KMP plugin.

## Этап 1. Тестовая инфраструктура

После успешного spike:

1. Постоянно добавить `include(":screenshot-tests")` в `settings.gradle.kts`.
2. Добавить version-catalog aliases для Roborazzi plugin и test dependencies; объявить plugin с `apply false` в корневом `build.gradle.kts`.
3. Подключить к test harness все модули, содержащие выбранные previews: `core:ui`, `feature:home`, `feature:games`, `feature:profile`, `feature:stats`, три game/menu пары и `feature:remembernumbermenu`.
4. Настроить один tracked output root, например `screenshot-tests/src/test/screenshots`, и отдельный build-only каталог для actual/diff/report. Включить `separateOutputDirs`, чтобы Gradle 9 не запускал Roborazzi-задачи с общим output конкурентно.
5. Не добавлять generated actual/diff в Git: tracked остаются только reference PNG; все промежуточные файлы должны находиться под `build/`.
6. Добавить короткие команды в `README.md`:
   - `./gradlew :screenshot-tests:verifyRoborazziJvm` — проверить;
   - `./gradlew :screenshot-tests:compareRoborazziJvm` — сформировать diff без принятия;
   - `./gradlew :screenshot-tests:recordRoborazziJvm` — обновить goldens.

## Этап 2. Детерминированный набор сценариев

Не принимать все 36 previews вслепую. Сначала провести аудит и оставить небольшой набор, где каждый PNG защищает отдельный риск.

Начальная матрица:

| Область | Обязательные состояния |
| --- | --- |
| `core:ui` | `GameMenuItem`, длинный title/subtitle, tallest sibling card |
| Home | новый пользователь/empty, populated recent records, completed daily-training card |
| Games | полный статический каталог, compact width |
| Stats | empty, rich/populated, one-mode, dark; один длинный экран с фиксированной высотой |
| Profile | new user, customized dark compact, tablet/near-next-rank |
| Standalone menus | Baspa, KenKoz, Sansprint default; Sansprint dialog и tablet |
| Game screens | initial, active/input, success/error для Number Sprint, Wide Eye и Don't Tap |
| Локали | один text-heavy phone screen для default, `ru` и `kk`; дополнительно compact `kk` как наиболее вероятный сценарий переполнения |

Правила для сценариев:

- Вызывать state-based leaf UI с фиксированным `UiState`, а не ViewModel, repository, clock или storage.
- Сохранять preview рядом с экраном в `commonMain`; не переносить demo fixtures в runtime repositories и не использовать их в production state.
- Каждый preview оборачивать в `OquTurboTheme`; screen size задавать через `@Preview(widthDp, heightDp)`.
- Явно задавать locale, dark mode и размеры; не полагаться на locale/timezone машины.
- Для анимаций и delayed UI использовать manual test clock/фиксированный момент capture. Если это невозможно, исключить конкретный preview до рефакторинга компонента, а не увеличивать глобальный threshold.
- Исключить состояния со случайными данными. Значения игры, таймер, score и выбранные элементы фиксировать в fixture.
- Не размножать все комбинации. Базовый viewport — один phone portrait; dark, tablet, compact и locale добавляются только там, где меняют layout-риск.

После стабилизации набора включить явную allow-list маркировку screenshot previews, чтобы новый обычный IDE preview не создавал golden автоматически. Предпочтительно использовать поддерживаемую Roborazzi include-аннотацию; если она требует main dependency во всех feature-модулях, вместо этого создать минимальную runtime-retained marker annotation в test-neutral package `core:ui` и настроить scanner по её FQCN.

## Этап 3. GitHub Actions

Создать отдельный `.github/workflows/pr-screenshot-tests.yml`:

1. Trigger: `pull_request` в `main`/`master` с path filters для `core/**`, `feature/**`, `resources/**`, `app/*/shared/**`, Gradle-файлов и самого workflow. Не запускать четыре одинаковых screenshot job по одному на продукт.
2. Environment: `ubuntu-24.04`, JDK 21, `LANG=C.UTF-8`, `LC_ALL=C.UTF-8`, `TZ=UTC`. Использовать уже закреплённый в `gradle/gradle-daemon-jvm.properties` JetBrains toolchain для фактического Gradle/test runtime и не использовать `ubuntu-latest`.
3. Настроить Gradle cache штатным `gradle/actions/setup-gradle`, не кэшируя generated screenshots как эталоны.
4. Запускать `xvfb-run -a ./gradlew :screenshot-tests:verifyRoborazziJvm --stacktrace`, если spike подтвердит необходимость X server; иначе запускать Gradle напрямую.
5. При любом результате сохранять JUnit/test result; при failure загружать Roborazzi HTML report, actual и diff images через `actions/upload-artifact` с коротким retention.
6. Добавить concurrency group по PR и отмену устаревшего run.
7. После периода стабилизации сделать check required в branch protection.

Goldens должны записываться в том же Linux-окружении, где выполняется verify. Для этого добавить отдельный `workflow_dispatch` workflow `update-screenshot-goldens.yml`: он запускает `recordRoborazziJvm` на выбранной ветке и отдаёт каталог tracked references artifact-ом. Разработчик скачивает artifact, просматривает изображения и коммитит их обычным PR. Workflow не должен сам пушить изменения или принимать визуальный diff.

Локальная запись на macOS/Windows допускается только для предварительного просмотра; такие PNG нельзя принимать как CI baselines без повторной генерации в Linux. Если даже на pinned `ubuntu-24.04` появится массовый шум после обновления runner image, следующий шаг — собственный GHCR container image с зафиксированными JDK, native libraries и fonts, используемый одновременно verify и record workflows.

## Этап 4. Baseline и rollout

1. Первый PR инфраструктуры содержит 2–3 pilot goldens и доказательство успешного CI verify.
2. Второй PR добавляет основную матрицу небольшими группами: core/home/games, stats/profile, затем standalone/game screens.
3. Каждый baseline PR должен содержать:
   - список добавленных/изменённых сценариев;
   - ссылку на CI artifact или приложенный contact sheet;
   - объяснение ожидаемых визуальных изменений.
4. Не принимать массовое обновление PNG без просмотра diff. Изменение golden и production UI в одном PR допустимо, но reviewer должен видеть обе части.
5. Начать со строгого сравнения. Допуск добавлять только после измеренного platform noise и локально для проблемного сценария; глобальный threshold не должен скрывать реальные изменения текста, spacing или цвета.

## Затрагиваемые файлы

Ожидаемые изменения при реализации:

- `settings.gradle.kts` — test-only module;
- `build.gradle.kts` — Roborazzi plugin `apply false`;
- `gradle/libs.versions.toml` — pinned plugin/library versions;
- `screenshot-tests/build.gradle.kts` — JVM 17 test harness, dependencies и Roborazzi configuration;
- `screenshot-tests/src/test/screenshots/**` — tracked reference PNG;
- существующие `*Preview*.kt`/screen-файлы — только marker и недостающие deterministic previews;
- `.github/workflows/pr-screenshot-tests.yml` — PR verification;
- `.github/workflows/update-screenshot-goldens.yml` — Linux baseline generation;
- `README.md` — команды и правила обновления;
- `.gitignore` — только если Roborazzi создаёт промежуточные файлы вне уже игнорируемых `build/`.

## Проверка реализации

После Kotlin/Gradle изменений выполнить:

```text
./gradlew ktlintCheck
./gradlew :screenshot-tests:verifyRoborazziJvm
./gradlew :core:ui:jvmTest :feature:home:jvmTest :feature:stats:jvmTest
./gradlew :app:oquturbo:desktopApp:jar :app:sansprint:desktopApp:jar
./gradlew :app:oquturbo:androidApp:assembleDebug :app:kenkoz:androidApp:assembleDebug :app:baspa:androidApp:assembleDebug :app:sansprint:androidApp:assembleDebug
```

Набор узких compile-задач уточнить по реально затронутым feature-модулям. Поскольку previews находятся в `commonMain` и используются несколькими продуктами, проверить минимум JVM и Android consumers; iOS не объявлять проверенным без Apple tooling.

Дополнительно:

- изменить один заведомо видимый цвет/spacing и убедиться, что CI падает и создаёт readable diff;
- вернуть изменение и получить зелёный verify;
- дважды выполнить verify на чистом checkout Linux без изменения PNG;
- проверить все три locale preview и отсутствие missing glyphs;
- выполнить `git diff --check`, `git diff`, `git status` и убедиться, что в commit нет actual/diff/build output.

## Риски и меры

- **Experimental preview scanner.** Ограничить его одним test-only модулем и закрыть compatibility spike до массовой разметки.
- **Разница рендера по ОС.** Record и verify только на одинаковом Linux runner; при шуме перейти на pinned container.
- **Default system fonts.** Проверить glyph/render stability на spike. Не подменять production typography только ради тестов; при системном шуме фиксировать CI container/font packages.
- **Анимации и время.** Manual clock и state fixtures; не лечить flaky test широким threshold.
- **Большой объём PNG.** Curated allow-list, один сценарий на риск, Git LFS пока не вводить. Пересмотреть LFS только после измерения размера history.
- **Долгий CI.** Один централизованный JVM job, package filtering и Gradle cache; позже разбивать задачу только по измеренному времени.
- **Ложное ощущение platform coverage.** В README явно указать, что Desktop goldens защищают shared UI, но не заменяют Android/iOS end-to-end проверки.

## Definition of done

- Один documented Gradle command локально и в GitHub Actions проверяет curated screenshot suite.
- PR с намеренным визуальным изменением падает и публикует reference/actual/diff плюс HTML report.
- Goldens записываются в Linux через manual workflow и проходят review перед commit.
- Покрыты основные shared screens, standalone menu variants, light/dark, phone/compact/tablet и `values`/`values-ru`/`values-kk`.
- Нет зависимостей от реального storage, network, clock или random.
- Production module visibility и архитектурная dependency direction не ухудшены.
- `ktlintCheck`, узкие JVM/Android compile tasks, `git diff --check` и повторный clean-checkout verify проходят.

## Источники для проверки решения

- [Roborazzi: Compose Desktop preview support, generated tasks, reports and CI caveats](https://github.com/takahirom/roborazzi)
- [Roborazzi issue: Android Gradle Library Plugin for KMP host tests](https://github.com/takahirom/roborazzi/issues/757)
- [Android Developers: Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing)
- [Android Developers: Screenshot testing overview](https://developer.android.com/training/testing/ui-tests/screenshot)
