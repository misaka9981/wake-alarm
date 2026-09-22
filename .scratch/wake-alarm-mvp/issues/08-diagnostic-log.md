# 08: Diagnostic Log

**What to build:** Because cloud-built sideloaded builds have no device logging, the app keeps its own record of what each Alarm actually did and shows it in a page the owner can read on the phone. Any missed or misbehaving Alarm becomes explainable.

**Blocked by:** 05 (An Alarm fires reliably and is dismissed only by Challenge and Anchor)

Status: ready-for-human

- [x] The log records, per Alarm firing: scheduled time, actual fire time, permission state at fire, signalling volume, challenge duration, wrong-answer count, and whether the Escape Hatch was used.
- [ ] Log entries are persisted and survive an app restart.
- [ ] The log is readable in-app on the phone.
- [x] The page shows a bounded recent history rather than growing without limit.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `DiagnosticEntry` — one recorded Alarm firing: `alarmId`, `scheduledTime`
    (`null` when unknown), `firedAt`, `missingRequirements` (the permission state
    at fire), `signallingVolume` (alarm stream percentage at fire),
    `challengeDuration`, `wrongAnswers`, and `outcome`. `escapeHatchUsed` is
    derived from the outcome. Every field is validated on construction.
  - `FiringOutcome` — how a firing ended: `Dismissed` (challenge **and** anchor),
    `CapExpired` (signalled out, left uncleared), or `EscapeHatch` (force-silenced).
  - `DiagnosticLog` — the `core` port the Android adapter implements.
  - `DiagnosticLogCodec` — the decidable part of persistence: a versioned,
    line-oriented format (Base64 Alarm id, ISO-8601 instants, sorted requirement
    names, millisecond duration) that round-trips and, critically, keeps only the
    most recent `MAX_ENTRIES` (50) on both encode and decode, so the history is
    bounded.
  - `FiringSummary` + `FiringSession.summary()` — the ringing duration and
    wrong-answer count retained after the firing ends. `DismissalSession` now
    keeps its wrong-answer count past dismissal so this survives.
- `:app` (thin adapter)
  - `DataStoreDiagnosticLog` — appends each entry via `DiagnosticLogCodec`,
    bounded at 50, and reads it back; unreadable data is discarded rather than
    crashing the page.
  - `DiagnosticLogScreen` — the in-app page, reachable from the Alarm list
    ("Diagnostic Log"), newest firing first: outcome, scheduled vs actual time,
    signalling duration and volume, wrong-answer count, and the permissions
    missing at fire.
  - `AlarmFiringActivity` — records one entry per firing, before the screen
    closes: `Dismissed`, `EscapeHatch`, or `CapExpired` (recorded when the cap
    first expires and not overwritten if the Alarm is later cleared). It observes
    the fire instant, the reliability grants through `ReliabilityCheck`, and the
    alarm stream volume.
  - `AlarmScheduler` / `AlarmReceiver` — carry the exact armed instant through to
    the firing screen as `EXTRA_SCHEDULED_AT`, so the scheduled time is the real
    one rather than a recomputation; a development firing leaves it unknown.

How it was verified:

- `./gradlew :core:test` green: 169 tests, of which 14 are new — `DiagnosticLogCodecTest`
  (9), `DiagnosticEntryTest` (3), and two `FiringSessionTest` cases for
  `summary()`. The codec tests round-trip entries (including a null scheduled
  time and multiple missing requirements), derive `escapeHatchUsed`, bound the
  history on both encode and decode, and reject a malformed header/record/instant/
  requirement. The summary tests show the ringing time and wrong-answer count,
  including after dismissal.
- `./gradlew :app:assembleDebug` green; APK produced.

Acceptance:

- "Records the listed facts per firing" and "shows a bounded recent history" are
  covered by the `DiagnosticEntry`/`DiagnosticLogCodec` tests and the recording
  wiring, so those boxes are ticked.
- "Persisted and survives an app restart" and "readable in-app on the phone" are
  implemented through the DataStore adapter and the screen, by the same pattern
  already verified for the Escape Hatch log, but both can only be confirmed on
  the phone, so those boxes are left unchecked.

Pending device confirmation（待机主真机确认）:

- On the phone, open "Diagnostic Log" from the Alarm list and read the record
  written by one real firing (or the DEV: Fire Alarm path), confirming every
  field reads sensibly.
- Confirm the record is still listed after force-stopping and reopening the app
  (it must survive a restart).
- Let an Alarm ring past the sound cap and confirm it is recorded as
  "Sound cap expired — left uncleared", and that a force-silenced Alarm is
  recorded as "Escape Hatch used".
