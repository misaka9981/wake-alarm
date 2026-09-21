# 02: Create and persist an Alarm

**What to build:** The owner can manage their Alarms. They create an Alarm with a time and the days of the week it repeats on, edit it, enable or disable it, and delete it. The configuration survives an app restart, so Alarms are not lost.

**Blocked by:** 01 (Project skeleton, cloud build, and hand-install)

Status: ready-for-human

- [x] The owner can create an Alarm with a time and a selection of repeat days.
- [x] The owner can edit, enable, disable, and delete an Alarm.
- [x] Multiple Alarms can exist at once.
- [ ] Alarm configuration is persisted and survives an app restart.
- [ ] The list of Alarms is shown with each Alarm's time, days, and enabled state.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `Alarm` / `AlarmTime` / `AlarmId` — the domain model. An impossible clock time,
    or an Alarm with no repeat days, cannot be constructed.
  - `AlarmCatalog` — create, edit, enable/disable, and delete over an immutable,
    time-ordered list, so the management rules are testable without Android.
  - `AlarmRepository` — the persistence port `core` declares.
  - `AlarmCodec` — versioned, storage-agnostic encode/decode of the Alarm
    configuration; unreadable data raises `AlarmFormatException`.
- `:app` (thin adapter)
  - `DataStoreAlarmRepository` — persists the codec's string in Preferences
    DataStore. Unreadable data falls back to an empty list instead of crashing.
  - `AlarmListScreen` + `AlarmEditorDialog` — Compose list showing each Alarm's
    time, repeat days, and enabled switch, with a create/edit dialog (Material3
    time picker, day chips, enable toggle) and delete.

How it was verified:

- `./gradlew :core:test` green: 22 tests (AlarmCatalog 8, AlarmCodec 8, Alarm 5,
  AppInfo 1).
- `./gradlew :app:assembleDebug` green.
- CI: [run 35661844464](https://github.com/misaka9981/wake-alarm/actions/runs/35661844464)
  green — core tests, debug APK build, and Release publish all succeeded.

Acceptance:

- Create / edit / enable / disable / delete / multiple Alarms are covered by the
  `core` tests, so those boxes are ticked.
- Persistence and the rendered list are implemented and build, but can only be
  confirmed on the phone; those two boxes are left unchecked.

Pending device confirmation（待机主真机确认）:

- Create an Alarm, edit it, toggle it, delete it, and confirm the list shows each
  Alarm's time, days, and enabled state.
- Restart the app and confirm the Alarms are still there.

