# 14: UI/UX polish — Material 3 theme, app bars, and a card-based Alarm list

**What to build:** Presentation-only polish across `:app`. The screens work but
look bare. Apply a proper Material 3 theme (including dark mode), give every
screen a consistent top app bar, and make the Alarm list scannable. No behaviour
or decidable logic changes; `:core` is untouched.

**Blocked by:** None

Status: ready-for-human

- [ ] The app follows the system light/dark setting with a coherent Material 3
      colour scheme.
- [x] The Alarm list has a title bar and card-based Alarm rows (time prominent,
      repeat days, Silent Mode / difficulty hints, trailing enable switch), with
      the add action as a FAB.
- [x] Navigation links and debug entry points stay reachable and never overlap a
      row or its switch.
- [x] Settings, Statistics, Diagnostic Log, Reliability, and the debug screens
      share one consistent titled top app bar with back navigation, instead of
      their own headline plus Close/Back buttons.
- [x] Chinese remains the default, English still shows on an English locale.
- [x] `./gradlew :core:test :app:assembleDebug` is green.

## Comments

Implemented on branch `main`. Every change is presentation-only in `:app`; no
`:core` logic or test, no spec, and no ADR was touched, and no other ticket's
`Blocked by` was changed.

### Material 3 theme and dark mode

- New `ui/theme/Theme.kt` exposes `WakeAlarmTheme`. It chooses between
  `lightColorScheme()` and `darkColorScheme()` from `isSystemInDarkTheme()`, and
  on Android 12+ (`SDK_INT >= S`) uses the wallpaper's dynamic colour scheme
  (`dynamicLightColorScheme` / `dynamicDarkColorScheme`). It wraps
  `MaterialTheme(colorScheme = …, typography = Typography())`.
- `MainActivity` now renders `WakeAlarmTheme` instead of the bare
  `MaterialTheme`. `AlarmFiringActivity` was switched to `WakeAlarmTheme` too so
  the firing screen shares the same light/dark scheme.
- `values/themes.xml` became a transparent-status-bar, no-ActionBar light theme
  and a new `values-night/themes.xml` is its dark counterpart, so the window
  behind Compose matches the system setting.
- `MainActivity` calls `enableEdgeToEdge()` so the top app bars own their window
  insets on every supported Android version (Android 15 forces edge-to-edge
  anyway). The `OnboardingScreen` column got `statusBarsPadding()` +
  `navigationBarsPadding()` so its content stays clear of the system bars.

### Alarm list

- `AlarmListScreen` gets a `TopAppBar` titled with `R.string.app_name`. The
  reliability warning is a tinted `Warning` `IconButton`; the navigation links
  (Diagnostic Log / Statistics / Settings) and the debug entry points live in an
  overflow `DropdownMenu`, gated exactly as before (`BuildConfig.DEBUG` in
  `WakeAlarmRoot`; DEV items simply absent from the menu when their callback is
  null). Being the Scaffold's `topBar`, the bar owns its own region and cannot
  overlap a row or its switch; a transient menu is modal over the list by design.
- Each Alarm is now a `Card`: `headlineMedium` time, repeat days, and
  `bodySmall` Silent Mode / starting-difficulty hints, with the trailing enable
  `Switch`; the whole card is clickable to edit. Rows are spaced with a
  `LazyColumn` `contentPadding`/`Arrangement.spacedBy`.
- The add action is a `FloatingActionButton` with `Icons.Default.Add` and the new
  `alarm_add` content description.
- The empty state is a centred `Icons.Default.Notifications` icon plus the
  existing Chinese explanation.
- All existing callbacks keep their meaning; only layout changed.

### Consistent secondary-screen app bar

- New `ui/WakeAlarmTopBar.kt` renders the shared titled `TopAppBar` with an
  `Icons.AutoMirrored.Filled.ArrowBack` button that invokes the screen's original
  `onClose`.
- `SettingsScreen`, `StatisticsScreen`, `DiagnosticLogScreen`,
  `ReliabilityScreen`, `DismissChallengeScreen` (both ongoing and dismissed
  states), and `AnchorScreen` now use it, replacing their headline plus
  Close/Back buttons. Dialogues (Alarm editor, change password, Escape Hatch
  password) stay dialogues.

### Strings and vocabulary

- Added `alarm_add` and `nav_more` to **both** `res/values/strings.xml`
  (Chinese) and `res/values-en/strings.xml` (English); key sets stay identical
  at 152 keys each. No new hard-coded UI copy was introduced and no Snooze
  concept was added.

### Verification

- `JAVA_HOME=$(brew --prefix openjdk@17) ./gradlew :core:test :app:assembleDebug`
  — BUILD SUCCESSFUL; `:core` tests green.
- `diff` of the `name="…"` sets of the two `strings.xml` files reports no
  difference (152 keys each).
- `rg` for `Text(text = "…")` in `app/src/main/kotlin` finds only the
  pre-existing `+` / `−` difficulty stepper symbols, and `Snooze` appears only
  in comments that state its deliberate absence.

### Left to confirm on the real device (待机主真机确认)

- Checkbox 1 stays unchecked: the dark-mode scheme selects correctly in code and
  builds, but the **actual dark rendering / dynamic colour look** needs the
  owner's device.
- A visual pass of the new cards, empty state, FAB, and shared app bars on the
  phone is also worth confirming by eye; the structure was verified by build.
- The system-bar inset/edge-to-edge appearance (status-bar icons over the
  transparent bar) needs a device look.
