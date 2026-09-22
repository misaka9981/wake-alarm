# 12: Per-Alarm default difficulty

**What to build:** The owner sets, the night before, the difficulty an Alarm should start at when it fires. Escalation then continues from that level.

**Blocked by:** 03 (Dismiss Challenge)

Status: ready-for-human

- [x] Each Alarm has a configurable default difficulty.
- [x] The chosen difficulty is the level Escalation starts from when the Alarm fires.
- [x] The setting is set in advance, not during the Alarm.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `Alarm.defaultDifficulty` — the per-Alarm difficulty Escalation starts from,
    an `Int` defaulting to `Alarm.DEFAULT_DIFFICULTY` (1, the gentlest start). It
    is validated against `Alarm.DEFAULT_DIFFICULTY_RANGE` (1..10), whose top
    matches `EscalationPolicy`'s default maximum, so a chosen start is never more
    than the hardest challenge the policy escalates to. It is only the starting
    point; `DismissalSession`/`FiringSession` still raise it with elapsed time and
    wrong answers, and the owner has no way to change it during an Alarm.
  - `AlarmCodec` — the persisted format is now version 3 and carries the default
    difficulty after the Silent Mode flag. Version 2 data still decodes with the
    default difficulty at 1 and Silent Mode preserved; version 1 data still
    decodes with Silent Mode off and the default difficulty at 1, so an installed
    configuration is not lost on upgrade. A malformed or out-of-range difficulty,
    and a v3 record with the v2 field count, are rejected.
- `:app` (thin adapter)
  - `AlarmFiringActivity` — starts `FiringSession` from
    `loadedAlarm.defaultDifficulty`, so the morning's first challenge sits at the
    level the owner chose. The decision lives in `core`; the adapter only reads
    the field.
  - `AlarmListScreen` — the Alarm editor has a "Default difficulty (where
    Escalation starts)" stepper bounded by `Alarm.DEFAULT_DIFFICULTY_RANGE`, and
    the row shows "Starts at difficulty N" when N is above the default. It is set
    only here, while editing, never on the firing screen.

How it was verified:

- `./gradlew :core:test :app:assembleDebug --rerun-tasks` green: 188 core tests,
  of which 10 are new — `AlarmTest` (3), `AlarmCodecTest` (5), `FiringSessionTest`
  (1), plus the existing `DismissalSession`/`EscalationPolicy` coverage of the
  base difficulty is untouched and still green. The codec tests cover a v3 round
  trip with a non-default difficulty, decoding v2 data with the gentlest default,
  a v3 record with the v2 field count, and a malformed and out-of-range
  difficulty. `AlarmTest` covers the default, per-Alarm values, and the range
  guard. `FiringSessionTest` proves the firing's first challenge sits at the given
  base difficulty and escalates from there.
- `:app:assembleDebug` green; APK produced.
- CI: [run 35672231005](https://github.com/misaka9981/wake-alarm/actions/runs/35672231005)
  green — core tests, debug APK build, and Release publish all succeeded.

Acceptance:

- All three boxes are decidable and covered by the `core` tests and the adapter
  wiring above, so all three are ticked.

Pending device confirmation（待机主真机确认）:

- Edit an Alarm on the phone and raise its default difficulty, then fire it (or
  use "DEV: Fire Alarm") and confirm the first Dismiss Challenge shows the chosen
  difficulty and climbs from there.
- Confirm an Alarm left at the default still starts at difficulty 1, and that an
  existing installed configuration survives the upgrade without losing its
  Alarms.

