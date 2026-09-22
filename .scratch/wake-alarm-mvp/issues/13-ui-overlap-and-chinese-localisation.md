# 13: Fix Alarm-list overlap and localise the UI to Chinese

**What to build:** Two owner-facing fixes.

1. On the Alarm list, the navigation links (`Diagnostic Log`, `Statistics`,
   `Settings`) and the debug entry points are drawn *on top of* the Alarm rows,
   so the top row's text and its enable `Switch` sit underneath the clickable
   strings. Move every navigation control into a dedicated top region of the
   screen so nothing overlaps a row, its text, or its switch.
2. Every user-facing string is hard-coded English inside Kotlin. Extract them to
   string resources and make the app display Chinese by default, with English
   still available on an English-locale device.

**Blocked by:** None

Status: ready-for-human

- [ ] The Alarm list has no overlap: the navigation links (and the debug entry
      points) occupy their own space and never cover an Alarm row or its switch.
- [x] All user-facing text displays in Chinese by default.
- [x] On a device whose locale is English, the same screens display in English.
- [x] The firing notification channel name/description/title are localised too.
- [x] No Snooze term appears anywhere; canonical domain vocabulary is translated
      consistently (see Comments).

## Comments

Implemented on branch `main`.

### Layout: navigation moved into the Scaffold's `topBar`

`AlarmListScreen` no longer draws the navigation links and debug entry points
with `Modifier.align(Alignment.TopStart/TopEnd)` over the `LazyColumn`. They were
extracted into `AlarmListNavigation`, passed to `Scaffold(topBar = …)`, and the
Alarm list is the Scaffold's body. The Scaffold reserves the top bar's height for
it, so the navigation/DEV controls and the rows are laid out in separate regions
and cannot overlap a row, its text, or its `Switch`. A `statusBarsPadding()` on
the bar keeps it clear of the system status bar. The debug entries remain gated on
`BuildConfig.DEBUG` in `WakeAlarmRoot`; every callback keeps its previous
meaning, and `DEV: Fire Alarm` still fires the first Alarm (its id is read from
the loaded catalog).

The one acceptance item the owner must confirm by eye on the phone is the actual
visual: **no-overlap appearance is 待机主真机确认** (checkbox left unchecked).

### Localisation

- Every user-facing hard-coded string in `app/src/main/kotlin/**` (screen titles,
  button labels, dialog text, content descriptions, notification copy, statuses,
  error messages, generated anchor labels) now comes from resources via
  `stringResource(R.string.…)` or `context.getString(R.string.…)`.
- **Default `res/values/strings.xml` is Chinese**; `res/values-en/strings.xml` is
  English with the same key set (150 keys each). A non-English locale therefore
  falls back to Chinese, and an English locale picks up the English file.
  `alarm_channel_name` / `alarm_channel_description` / `alarm_notification_title`
  / `app_name` are in both (default `唤醒闹钟`, English `Wake Alarm`; `:core`
  `AppInfo.NAME` stays the English constant).
- Weekday chips are fixed short names (`周一`…`周日`; English file `Mon`…`Sun`)
  selected from resources — `Locale.getDefault()` / `getDisplayName` is gone.
  "Every day" is the all-days-selected case (`每天` / `Every day`).
- Numbers, difficulty, duration, and volume stay readable and interpolated
  (`难度 %1$d`, `%2$d 秒`, `音量 %2$d%%`).
- Vocabulary is consistent with `CONTEXT.md`: Alarm→闹钟, Dismiss Challenge→解除
  挑战, Physical Anchor→实体锚点, Escape Hatch→应急出口, Escalation→难度升级,
  Streak→连续天数, Silent Mode→静音模式, Diagnostic Log→诊断日志. No Snooze term
  appears in any user-facing string.

### Verification

- `JAVA_HOME=$(brew --prefix openjdk@17) ./gradlew :core:test :app:assembleDebug`
  — BUILD SUCCESSFUL; `:core` tests green (no `:core` change was made).
- Inspected the merged resources in the debug build:
  `values/values.xml` → `app_name=唤醒闹钟`, `alarm_channel_name=闹钟`,
  `alarm_notification_title=闹钟正在响`; `values-en/values-en.xml` →
  `app_name=Wake Alarm`, `alarm_channel_name=Alarm`.
- `rg` over `app/src/main/kotlin` shows no remaining `Text(text = "…")` hard-coded
  UI copy (only the `+` / `−` symbols and numeric placeholders) and no `Snooze`
  token except the existing comments that state its deliberate absence.

### Left to confirm on the real device (待机主真机确认)

- The Alarm list's actual no-overlap appearance (checkbox 1 remains unchecked).
- On-device rendering of the Chinese default and the English locale, including
  the notification channel name/description/title, needs a device look.

No `:core` logic or test was touched; no spec, ADR, or other ticket's dependency
was modified.
