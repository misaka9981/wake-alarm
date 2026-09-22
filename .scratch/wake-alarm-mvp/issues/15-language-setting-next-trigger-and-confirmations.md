# 15: In-app language setting, next-trigger label, and destructive-action confirmations

**What to build:** Three owner-facing improvements. (1) A visible in-app language
setting so the owner can switch between following the system, Chinese, and
English without changing the phone language. (2) Each Alarm row shows when it
will next fire in friendly words. (3) Deleting an Alarm, and disabling the last
enabled Alarm, ask for confirmation first. Also show the build identifier in
Settings so the installed build is identifiable.

**Blocked by:** None

Status: ready-for-human

- [x] Settings has a language control with three choices: follow the system,
      Chinese, English; the choice is persisted and applies immediately without
      restarting.
- [ ] The selected language is applied consistently across every screen and the
      Alarm notification, not just newly composed text.
- [x] Each Alarm row shows the next firing time in relative words
      (today/tomorrow/weekday), derived from `core`.
- [x] Deleting an Alarm requires confirmation.
- [x] Disabling the last enabled Alarm requires confirmation.
- [x] Settings shows a build identifier that changes with each CI build.
- [x] `./gradlew :core:test :app:assembleDebug` is green.

## Comments

Implemented on branch `main`. `:core` gained only pure logic and tests; no spec,
ADR, or other ticket's `Blocked by` was touched, and no Snooze concept was
introduced.

### 1. In-app language setting

- `:core` adds `AppLanguage` (`System` / `Chinese` / `English`, each with the
  `explicitTag` a pinned choice applies), the `LanguageCodec` that encodes the
  choice to one string and parses it back (unrecognised data fails safe to
  `System`), and the `LanguageRepository` port. Covered by
  `LanguageCodecTest` (round trip, blank/unknown fallback, case-insensitive
  parsing, tags).
- `:app` adds `DataStoreLanguageRepository`, a Preferences DataStore adapter
  shaped exactly like `DataStoreEscapeHatchRepository`.
- New `ui/AppLanguageProvider` reads the choice once, computes the target
  `Locale` (`explicitTag` for a pinned choice, the device locale for `System`),
  and wraps the whole app in
  `CompositionLocalProvider(LocalContext provides …, LocalConfiguration provides
  …, LocalAppLanguage …, LocalSetAppLanguage …)`. `stringResource` resolves
  through `LocalContext`/`LocalConfiguration`, so the entire tree — titles, top
  bars, dialogs, and already-composed text — re-resolves on change with no
  restart. The provider wraps `WakeAlarmTheme`, so it covers every screen.
- `MainActivity` and `AlarmFiringActivity` (a separate activity with its own
  composition) both wrap their content in the provider.
- `SettingsScreen` gets a Language section with three `FilterChip`s; a selection
  updates the composition and persists through the repository immediately.
- No appcompat was introduced. Default resources stay Chinese, so `System` on a
  non-English device shows Chinese and on an English device shows English.
- Notification: `AlarmReceiver` loads the choice and passes it to `AlarmService`,
  which builds the notification through `Context.localizedFor(language)`, so the
  title/content are generated from a localised Context even outside the
  composition. The channel name is fixed at creation (platform behaviour) and is
  accepted as-is.

### 2. Next-trigger label

- `:core` adds `NextTriggerDay` (`Today` / `Tomorrow` / `Weekday(day)`), a pure
  function built on `AlarmSchedule.nextTrigger`; a disabled Alarm yields `null`.
  Covered by `NextTriggerDayTest` (today, tomorrow, later weekday, the
  just-passed weekly case, disabled).
- `AlarmListScreen` renders one line per enabled Alarm ("今天 07:00" /
  "明天 07:00" / "周三 07:00"); disabled Alarms show no line. The `:app` side only
  formats the words from resources.
- New keys `alarm_next_today` / `alarm_next_tomorrow` / `alarm_next_weekday` in
  both `values/strings.xml` and `values-en/strings.xml`; the two key sets remain
  identical (166 keys each, verified with `diff`).

### 3. Destructive-action confirmations

- `AlarmCatalog.isOnlyEnabled(id)` is the pure decision, with tests in
  `AlarmCatalogTest`.
- Deleting an Alarm now closes the editor and shows a confirmation dialog before
  `AlarmCatalog.delete`.
- Turning off the only enabled Alarm shows a confirmation before
  `setEnabled(false)`; disabling any other Alarm still happens immediately, so
  the owner is not nagged.
- New keys `confirm_delete_alarm_title` / `_message`,
  `confirm_disable_last_alarm_title` / `_message`, and `action_disable` in both
  string files.

### 4. Build identifier

- `:app/build.gradle.kts` injects `buildConfigField("String", "BUILD_IDENTIFIER",
  …)` from a Gradle-generated UTC timestamp (`yyyy-MM-dd HH:mm 'UTC'`), so every
  build — CI or local — differs without touching the CI workflow.
  `buildFeatures { buildConfig = true }` was already on.
- `SettingsScreen` shows "版本 0.1.0 · 构建 <timestamp>" from
  `BuildConfig.VERSION_NAME` / `BuildConfig.BUILD_IDENTIFIER`; new keys
  `settings_build` / `settings_build_value`.

### Verification

- `JAVA_HOME=$(brew --prefix openjdk@17) ./gradlew :core:test :app:assembleDebug`
  — BUILD SUCCESSFUL; `:core` tests green, including the new
  `LanguageCodecTest` (5 tests) and `NextTriggerDayTest` (5 tests).
- Generated `app/build/generated/source/buildConfig/debug/.../BuildConfig.java`
  contains `BUILD_IDENTIFIER`; a second build produced a later timestamp
  (`02:17 UTC` → `02:18 UTC`), proving it changes per build.
- `diff` of the `name="…"` sets of the two `strings.xml` files reports no
  difference (166 keys each).
- `rg` finds no `Snooze` except the existing comments/tests that state its
  deliberate absence, and no new hard-coded UI copy.

### Left to confirm on the real device (待机主真机确认)

- Checkbox 2 stays unchecked: the wrapper and the localised notification are
  verified by build and code review, but the **actual instant cross-screen switch
  and the rendered notification text** need the owner's phone. Checkbox 1's
  control, persistence, and wiring are covered by code, so it is checked.
