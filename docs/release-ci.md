# Релизы приложений в GitHub Actions

Процесс адаптирован из [dubovozki-kmp](https://github.com/alad1nks/dubovozki-kmp/blob/main/docs/release-ci.md):
ручное создание версии из `main`, отдельная release-ветка, проверки, подписанный Android AAB и iOS TestFlight.
Для каждого приложения есть собственные **Create release <app>** и **Release <app>**.
Общая реализация находится в [create-release-app.yml](../.github/workflows/create-release-app.yml)
и [release-app.yml](../.github/workflows/release-app.yml).

| App | Создать релиз | Собрать / повторить релиз | Android secrets prefix | iOS bundle ID |
|---|---|---|---|---|
| oquturbo | [Create release oquturbo](../.github/workflows/create-release-oquturbo.yml) | [Release oquturbo](../.github/workflows/release-oquturbo.yml) | `OQUTURBO_` | `com.alad1nks.oquturbo` |
| baspa | [Create release baspa](../.github/workflows/create-release-baspa.yml) | [Release baspa](../.github/workflows/release-baspa.yml) | `BASPA_` | `com.alad1nks.baspa` |
| kenkoz | [Create release kenkoz](../.github/workflows/create-release-kenkoz.yml) | [Release kenkoz](../.github/workflows/release-kenkoz.yml) | `KENKOZ_` | `com.alad1nks.kenkoz` |
| sansprint | [Create release sansprint](../.github/workflows/create-release-sansprint.yml) | [Release sansprint](../.github/workflows/release-sansprint.yml) | без префикса | `com.alad1nks.sansprint` |
| dualfocus | [Create release dualfocus](../.github/workflows/create-release-dualfocus.yml) | [Release dualfocus](../.github/workflows/release-dualfocus.yml) | `DUALFOCUS_` | `com.alad1nks.dualfocus` |
| numbertrail | [Create release numbertrail](../.github/workflows/create-release-numbertrail.yml) | [Release numbertrail](../.github/workflows/release-numbertrail.yml) | `NUMBERTRAIL_` | `com.alad1nks.numbertrail` |
| rotationmatch | [Create release rotationmatch](../.github/workflows/create-release-rotationmatch.yml) | [Release rotationmatch](../.github/workflows/release-rotationmatch.yml) | `ROTATIONMATCH_` | `com.alad1nks.rotationmatch` |
| ruleswitch | [Create release ruleswitch](../.github/workflows/create-release-ruleswitch.yml) | [Release ruleswitch](../.github/workflows/release-ruleswitch.yml) | `RULESWITCH_` | `com.alad1nks.ruleswitch` |
| symbolcount | [Create release symbolcount](../.github/workflows/create-release-symbolcount.yml) | [Release symbolcount](../.github/workflows/release-symbolcount.yml) | `SYMBOLCOUNT_` | `com.alad1nks.symbolcount` |
| wordflow | [Create release wordflow](../.github/workflows/create-release-wordflow.yml) | [Release wordflow](../.github/workflows/release-wordflow.yml) | `WORDFLOW_` | `com.alad1nks.wordflow` |

## Создание релиза кнопкой

После merge в `main`: **Actions → Create release <app> → Run workflow → Use workflow from: main**.
Номер вводить не нужно. Запуск из другой ветки завершится ошибкой до создания релиза.

1. Workflow получает актуальный `origin/main` и release-ветки.
2. Среди веток строго вида `release/<app>/X.Y` находит максимальную числовую версию и увеличивает minor:
   `1.9 → 1.10`, `2.0 → 2.1`. Другие приложения, вложенные ветки, `X.Y.Z` и суффиксы `-beta`
   в нумерации не участвуют. Результат сборки ветки значения не имеет.
3. Если веток приложения ещё нет, создаёт первую версию `1.0` без увеличения minor.
   Исходные `1.0` и `1.0.0` в `main` соответствуют этой версии.
   Если следующая версия меньше версии в `main`, запуск остановится: согласуйте историю версий вручную.
4. Создаёт `release/<app>/X.Y` **из main**, добавляя один коммит только в
   `app/<app>/androidApp/build.gradle.kts` и `app/<app>/iosApp/Configuration/Config.xcconfig`.
   Обе пользовательские версии становятся `X.Y`; Android `versionCode` — максимум для этого приложения
   среди `main` и числовых release-веток плюс один (до 2100000000). `main` и другие приложения не меняются.
5. Публикует новую ветку и явно вызывает `Release <app>` через `workflow_dispatch`.
   В Summary появляются ссылки на ветку и сборки.

Используется встроенный `GITHUB_TOKEN` с `contents: write` и `actions: write`.
Rulesets и настройки организации должны разрешать создание `release/**` и запуск workflow.
Push с `GITHUB_TOKEN` не запускает push-workflow, поэтому необходим явный dispatch.
Дополнительный PAT для Create release не нужен.

Создание релизов сериализовано отдельно для каждого приложения. Push с пустым lease не перезаписывает
существующую ветку, даже если её параллельно создал другой пользователь.
Не удаляйте числовые release-ветки: это история выделенных версий; магазины и теги не опрашиваются.

## Повторный запуск

Если ветка уже опубликована, но dispatch не удался, выберите **Re-run jobs** в том же Create release.
Маркер `Release-Workflow-Run` позволяет повторно использовать ветку. Если после создания в неё добавили
коммиты, Create release остановится: запускайте **Actions → Release <app> → Run workflow** на этой ветке.
Новый запуск кнопкой Create release выделяет следующую версию.
Повторный dispatch может создать дополнительную сборку, если предыдущий запрос уже был принят.

Release также запускается при обычном push в `release/<app>/**`. Ручной запуск на `main` или ветке
другого приложения завершится ошибкой проверки. При создании ветки вручную обновите версии самостоятельно:
имя ветки не меняет версии в файлах. Старые release-ветки используют workflow из своего коммита;
для новой автоматизации создавайте ветки после merge этого изменения.

## Этапы и результат

- `verify`: JVM-тесты выбранного shared-модуля и `core:data`.
- `android`: Android unit-тесты всех доступных вариантов через `test` и подписанный `bundleRelease`; артефакт `<app>-release`
  содержит `<app>.aab`. Отсутствующие секреты подписи приводят к ошибке, а не к публикации unsigned AAB.
- `publish-android`: сохраняет AAB в `alad1nks/alad1nks.github.io`, путь
  `oquturbo/<app>/main/<app>.aab`. Это существующая схема доставки, не автоматическая загрузка в Google Play.
  Публикация сериализована между приложениями; неизменившийся файл не создаёт пустой коммит.
- `ios`: shared-тесты iOS Simulator, Xcode Release archive, подписанный IPA и загрузка в App Store Connect.
  Runner `macos-26`; схема `iosApp`; framework берётся из `baseName` shared-модуля выбранного приложения.
  Настройки manual signing и пути framework меняются только в app Release-конфигурации временного checkout.
  Исходный Xcode-проект восстанавливается; provisioning profile и временный keychain удаляются.

Android и iOS стартуют после общих проверок независимо. Ошибка iOS не блокирует доставку успешного AAB;
общий workflow успешен только при успехе всех jobs. Обычные PR и push в `main` релизы не публикуют.
Web/Desktop-деплой остаётся в существующих workflow.

## Секреты GitHub Actions

Добавляйте **Repository secrets**: Settings → Secrets and variables → Actions.
Environment-only secrets не используются. Существующие ключи Android-подписи сохраняются.

### Android

К каждому имени добавьте префикс из таблицы приложений. Например, для Baspa это
`BASPA_KEYSTORE_BASE64`, `BASPA_KEY_ALIAS`, `BASPA_KEY_PASSWORD`, `BASPA_KEYSTORE_PASSWORD`.
Для OquTurbo создайте `OQUTURBO_KEYSTORE_BASE64`, `OQUTURBO_KEY_ALIAS`,
`OQUTURBO_KEY_PASSWORD`, `OQUTURBO_KEYSTORE_PASSWORD` с текущими значениями подписи OquTurbo.
Эти имена используются в release, PR и build-and-deploy workflow OquTurbo.
SanSprint сохраняет общие имена без префикса; не удаляйте их при переносе OquTurbo.

| Имя без префикса | Значение |
|---|---|
| `KEYSTORE_BASE64` | Base64 release keystore `.jks`, соответствующего опубликованному приложению |
| `KEY_ALIAS` | Alias ключа |
| `KEY_PASSWORD` | Пароль ключа |
| `KEYSTORE_PASSWORD` | Пароль keystore |

Общий `ACCESS_TOKEN`: token с Contents: Read and write в `alad1nks/alad1nks.github.io`.

### iOS / TestFlight

| Имя | Значение |
|---|---|
| `IOS_DISTRIBUTION_CERTIFICATE_BASE64` | Base64 Apple Distribution `.p12` с приватным ключом |
| `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD` | Непустой пароль экспорта `.p12` |
| `<APP>_IOS_PROVISIONING_PROFILE_BASE64` | Отдельный App Store Connect profile для bundle ID приложения; например, `OQUTURBO_IOS_PROVISIONING_PROFILE_BASE64` |
| `APPLE_TEAM_ID` | Team ID Apple Developer |
| `APP_STORE_CONNECT_API_KEY_ID` | Key ID командного API-ключа |
| `APP_STORE_CONNECT_API_ISSUER_ID` | Issuer ID команды |
| `APP_STORE_CONNECT_API_KEY_BASE64` | Base64 приватного `AuthKey_<KEY_ID>.p8` |

Для каждого из десяти приложений нужен свой `<APP>_IOS_PROVISIONING_PROFILE_BASE64`, где `<APP>` —
ID из таблицы в верхнем регистре. GitHub обращается к именам secrets без учёта регистра.
Сертификат, команда и API-ключ общие. Firebase-секреты из примера здесь не нужны.

Для OquTurbo, Baspa, KenKoz, SanSprint и WordFlow удалены прежние суффиксы имён приложений
из bundle ID. Перед релизом подготовьте App ID, запись App Store Connect и provisioning profile
для нового ID из таблицы, затем обновите соответствующий `<APP>_IOS_PROVISIONING_PROFILE_BASE64`.
Старый профиль не подходит новому ID; сборка с новым ID не является обновлением приложения со старым ID.

Первичная настройка:

1. В действующей Apple Developer Program зарегистрируйте explicit App ID из таблицы и создайте
   соответствующее приложение в App Store Connect. Все app bundle ID имеют вид `com.alad1nks.<app>`;
   `APPLE_TEAM_ID` задаёт команду подписи, не добавляется к bundle ID.
2. Экспортируйте Apple Distribution certificate вместе с приватным ключом в `.p12`.
   Для каждого App ID создайте App Store Connect provisioning profile с этим сертификатом.
   Скрипт проверяет команду, bundle ID, срок действия, тип профиля и соответствие сертификата.
3. Создайте Team API key App Store Connect с правом загрузки сборок (роль Developer), сохраните
   Key ID, Issuer ID и `.p8`. Individual API key этим процессом не поддерживается.
4. Сохраните секреты. В TestFlight настройте Internal Testing, тестировщиков и автоматическую раздачу.
   Обработка Apple асинхронная; Missing Compliance и external Beta App Review требуют действий владельца.

Base64 на macOS: `base64 -i /path/to/file | tr -d '\n' | pbcopy`.
На Linux: `base64 -w 0 /path/to/file`. Кодируйте `.jks`, `.p12`, `.mobileprovision`, `.p8`;
пароли и идентификаторы сохраняйте обычным текстом. Не добавляйте файлы ключей в Git.

## Номера iOS и диагностика

`MARKETING_VERSION` синхронизируется с Android при создании ветки. `CFBundleVersion` задаётся в CI:
`(run_number / 100 + 1).(run_number % 100).run_attempt`, деление целочисленное.
Запуск 123 → `2.23.1`, повтор → `2.23.2`. Нумерация каждого Release workflow независима;
поддерживаются run_number до 999899 и attempt до 99.
Перед первой загрузкой сравните номер с ранее опубликованными сборками. Повтор старого запуска после
нового может быть отклонён Apple; повторяйте актуальный. Не переименовывайте Release workflow без сверки
нумерации с App Store Connect.

Артефакт `<app>-ios-release-<run_number>-<run_attempt>` хранит IPA, dSYM, логи, xcresult и отчёты
тестов 14 дней, включая доступную диагностику при ошибке. Ключи в артефакты не входят.
Успешная загрузка означает принятие файла Apple, а не завершение обработки или публикацию в App Store.

## Проверка изменений и первого релиза

```shell
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s .github/scripts -p 'test_*release.py' -v
bash -n .github/scripts/ios-release.sh
actionlint .github/workflows/*release*.yml
```

Тесты используют временные Git-репозитории: все приложения, изоляция версий, числовая сортировка,
максимальный versionCode, повторный запуск и отказ при изменённой ветке. Проверяется конфигурация
подписи и отклонение неверных профилей. PR workflow запускает их на macOS, включая преобразование
реальных Xcode-проектов через `plutil`; на Linux эта проверка явно пропускается.

После merge настройте secrets, запустите Create release нужного приложения, проверьте коммит только
двух файлов версий, успешные jobs, AAB в артефактах и целевом репозитории, затем IPA и обработку TestFlight.
Сам PR не запускает публикацию. Полную подпись и доставку можно подтвердить только реальным релизом
с действующими secrets; unit-тесты и проверка YAML этого не доказывают.
