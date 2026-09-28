# UI design ownership

OquTurbo retains its purple Material language, localized content and game-specific boards. Use these owners when
changing shared presentation; do not copy a card or stage surface into a feature.

## Foundation and recipes

- `core/designsystem/.../theme/OquTurboTheme.kt` owns palette, typography and shapes. Generic chrome uses
  `MaterialTheme.shapes`: extraSmall for small badges, medium for nested tiles/back actions, large for page/menu/
  result/pause surfaces, extraLarge for ready panels. Circular medals and artwork geometry remain intentional.
- `core/designsystem/.../theme/OquTurboLayout.kt` owns page gutter (24dp), compact game gutter (16dp), page width
  (760dp), play/reading width (560dp), interruption width (420dp), related-text gap (8dp), tight gap (4dp),
  standard gap (16dp), section gap (24dp), card inset (20dp), stage inset (24x28dp), action minimum height (56dp),
  back action (48dp), header action/icon (52/26dp), and menu icon tile/icon (56/28dp).
- `core/ui/.../component/AppCard.kt` owns neutral and compact cards, primary/secondary accents and subdued cards.
  It preserves Material Card behavior, disabled states, outline and elevation. Features own padding and content.
  Use the clickable overload for a whole-card action. `containerColor` is an escape hatch for persisted user
  personalization (Profile's Twilight background); ordinary cards choose a semantic tone.
- `GameStagePanel.kt` owns Ready/Paused surfaces and their centered column, padding and gaps. Callers own width,
  scrolling, localized text, enabled controls and live-region semantics. Apply `widthIn` **before** `fillMaxWidth`.
- `GameHeader.kt` preserves centered scores when measured content fits, uses a single row with an offset score
  when the total widths fit, and stacks only when necessary. Stacked values get the full width and a fitting theme
  heading style (at least titleLarge), preserving font scale and whole numbers. Neither value is ellipsized. Overlay-style game screens reserve the actual header height outside the scroll viewport: apply the measured
  top inset, then clipToBounds, then verticalScroll. Top padding inside a scrollable column does not separate
  content from a fixed transparent header.
- `GameMenuItem.kt`, `AppBackButton.kt`, `AppTopBar.kt`, `PageHeader.kt` and `GameScoreBadge.kt` own shared chrome.
  Top bars remain transparent; page headers keep headlineMedium/bodyLarge hierarchy. AppTopBar and bottom
  navigation intentionally span the viewport; pageMaxWidth caps page and GameHeader content, not this viewport chrome.
- `GameResultCard.kt` keeps its high surface, 2dp tonal elevation and 24x20dp content inset. `GameStateOverlay.kt`
  keeps its 0.32 scrim, highest surface, 6dp tonal elevation, 72/36dp icon treatment and 28x32dp content inset.
  `AnimatedGameStateOverlay.kt` retains its existing animation and input contract. Inline stages and blocking
  overlays are distinct recipes.

Keep a page's existing Column/LazyColumn and inset behavior. Center 760dp page content, use the shared gutter and
let text/actions grow. Do not impose page geometry on board cells, tap targets, art, charts or animations.
New tokens need actual reuse or a clear semantic role; new components need demonstrated multiple consumers.

## Review and migration inventory

Paths below are relative to each module's `src/commonMain/kotlin/com/alad1nks/oquturbo/feature/<module>/ui` unless
stated otherwise. “Retained” means source review confirmed the existing distinct role, not new runtime evidence.

| Family | Final disposition and source | Deterministic coverage |
| --- | --- | --- |
| Shared components | Changed owners above; shape/spacing tokens and two small presentational wrappers | `DesignGalleryLightPreview`, `DesignGalleryDarkPreview`, compact large-text gallery; long-header en/ru/kk; dark compact overlay; existing menu previews |
| Home | `HomeScreen.kt`: neutral AppCard and primary training tone, page metrics | empty/populated/training Home previews |
| Training complete | `DailyTrainingCompleteScreen.kt`: shared page metrics; retained 440dp celebration card and 32dp radius hierarchy | `DailyTrainingCompletePreview` (now screenshot-tagged) |
| Games | `GamesScreen.kt`: one wrapping AppCard for all ten titles/descriptions, wrapping skill tags; theme chrome shapes; artwork untouched | existing catalog previews |
| Stats overview | `StatsScreen.kt` and `StatsSections.kt`: neutral empty/section cards and page metrics; chart and filter semantics retained | empty/rich/dark/compact Stats previews |
| Stats drill-down/history | `StatsDetailScreen.kt`: neutral card, effective centered page cap; existing callback/filter/history content retained | existing game/mode detail previews |
| Profile overview | `ProfileScreen.kt`, `ProfileSections.kt`, `ProfileHeroCard.kt`: neutral/compact/secondary cards and page metrics; persisted customized hero color retained | default/customized/dark/compact Profile previews |
| Profile children/dialogs | `ProfileChildScreens.kt`: rank current accent, compact unlock/title/settings cards and page metrics; edit/confirm/dismiss, selected and locked semantics retained | `ProfileEditPreview`, `ProfileRanksPreview`, `ProfileAchievementsPreview`, `ProfileTitlesPreview`, `ProfilePersonalizationPreview`, `ProfileSettingsPreview`, dark large-text settings and `ProfileLanguageOptionsPreview` (Kazakh options content) |
| Hub navigation | `app/oquturbo/shared/.../ui/OquTurboNavigationBar.kt`: retained theme-backed Material navigation colors and visibility/insets | hub runtime acceptance; no replacement scaffold |
| Number Sprint menu/custom dialog | `remembernumbermenu`: shared page metrics and GameMenuItem; custom digit field/selection geometry retained | phone/tablet menu and custom dialog previews |
| Wide Eye menu | `kenkozgamemenu`: shared metrics/menu row, effective centered width cap | existing menu preview |
| Don't Tap menu | `baspagamemenu`: shared metrics/menu row; existing centered scroll retained | existing menu preview |
| Memory Grid menu | `memorygridmenu`: shared metrics/menu row; existing centered LazyColumn retained | `MemoryGridMenuPreview` (module now included by screenshot-tests) |
| Number Sprint | `remembernumber`: shared header/menu/result/overlay, actual header clearance; keypad/digit/timing and training controls retained | ready/active/result/record/training/compact previews and existing retry tests |
| Wide Eye | `kenkozgame`: shared header/menu/result/overlay and chrome metrics; peripheral target geometry retained | existing mode/state previews and retry tests |
| Don't Tap | `baspagame`: shared header/menu/result/overlay metrics; tap/reaction/stop semantics retained | existing state previews |
| Memory Grid | `memorygrid`: shared header/score/result metrics and header clearance; grid/sequence/reverse/flash/accepted-tap geometry retained | existing state previews and logic tests |
| Word Flow | `wordflow`: Ready/Paused GameStagePanel, header clearance, action metrics; sentence/choices/timer retained | ready/paused/result/error previews; compact pause/resume, single-line long-record and all-three-Kazakh-answers-visible semantics tests |
| Dual Focus | `dualfocus`: Ready/Paused GameStagePanel, compact ready targets stack; active lanes untouched; header clearance | existing ready/paused/active/result previews, compact Kazakh large-text ready and `DualFocusReadyScrolledCompactLargeTextPreview` (320x640, Russian, font scale 1.5) |
| Rotation Match | `rotationmatch`: Ready/Paused GameStagePanel and header clearance; selector/grid and compact gutter retained | existing ready/paused/active/result/error and semantics cases |
| Number Trail | `numbertrail`: Ready/Paused GameStagePanel; compact header variant and trail geometry retained | existing ready/paused/board/result/error and semantics cases |
| Symbol Count | `symbolcount`: Ready/Paused GameStagePanel; compact header variant, icons and answers retained | existing ready/paused/board/result/error and semantics cases |
| Rule Switch | `ruleswitch`: Ready/Paused GameStagePanel; compact header variant, rule cue and answers retained | existing ready/paused/active/result/error and semantics cases |

State coverage stays game-specific: absent pause/loading/error states are not invented. Legacy centered score/record
headers and newer compact score/best tiles intentionally use different space, while sharing palette/type/shape roles.
Menus retain their existing per-content vertical rhythm. Material dialogs, chips, bottom navigation, chart cells,
selected/locked rank indicators, board targets and illustration shapes remain feature/Material-owned.

## Consumers and validation

All current `app/<product>/shared` consumers inherit `MainScreen` / `OquTurboTheme`: `oquturbo`, `sansprint`, `kenkoz`,
`baspa`, `wordflow`, `dualfocus`, `rotationmatch`, `numbertrail`, `symbolcount`, `ruleswitch`. There is no Memory Grid
standalone root. Changes stay in commonMain; navigation, DI, storage, IDs, session rules and resources are unchanged.

For palette/shape/card/stage changes, update the shared gallery in `core/ui/.../component/DesignSystemPreviews.kt`
and review representative changed feature renders against the previous baselines before recording approval.
Long labels and numeric records need compact 320dp, ordinary 390dp, wide 760–800dp and 1.5-font-scale checks;
light/dark galleries supplement, not replace, feature rendering and interaction checks. Keep all existing screenshot
cases. Profile child routes collect live state and pass callbacks to previewable leaf composables; previews reuse existing
ProfileDemoData without repositories or fake ViewModels. Word Flow's focused UI tests exercise reachable actions and
layout with long numbers; other games' existing
semantics tests guard callbacks, disabled/loading/save-error behavior and scrolling.

Run `QUALITY_GATES.md` checks, including all ten JVM/Wasm consumers. Android/browser/iOS runtime acceptance requires
actual execution and must be distinguished from compilation and deterministic JVM rendering in the PR evidence.

The language selector snapshot covers the real `LanguageOptions` content reused by `AlertDialog`, following Number
Sprint's content-preview pattern. It does not claim popup-window, dismiss or focus acceptance: those require runtime
QA. The desktop preview scanner captures one root and cannot capture an AlertDialog's additional window directly.

The Dual Focus JVM scroll regression follows the existing Word Flow Compose UI test setup (test-only uiTest and
Desktop runtime dependencies, unchanged version catalog). It scrolls the 320x640 / 1.5-font-scale ready screen to
Start, asserts that every visible content text stays below the fixed header, and verifies the action fires once.
The scrolled preview controls the real ScrollState and waits for its measured maximum; production uses the default
remembered state. Word Flow, Rotation Match and Memory Grid result scrolling use the same viewport ordering.
Number Sprint does not have this vertical-scroll parent; Number Trail, Symbol Count and Rule Switch scroll their
headers with content and do not require fixed-header separation.
