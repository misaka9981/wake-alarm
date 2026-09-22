# 11: Silent Mode per Alarm

**What to build:** A per-Alarm Silent Mode so the owner can be woken without disturbing someone else. In Silent Mode the Alarm signals by vibration only, with no sound. It changes only how the owner is signalled — never how hard the Alarm is to dismiss.

**Blocked by:** 05 (An Alarm fires reliably and is dismissed only by Challenge and Anchor)

Status: ready-for-human

- [x] Each Alarm has a Silent Mode toggle, off by default.
- [ ] A Silent Mode Alarm signals by vibration only, with no sound.
- [x] Dismissal mechanics, escalation, and Streak rules are identical to a ringing Alarm.
- [ ] The sound cap becomes a vibration cap in Silent Mode.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `Alarm.silentMode` — the per-Alarm Silent Mode setting, a `Boolean` defaulting
    to `false`, so a normal Alarm still rings and Silent Mode is never the default.
  - `Signalling` — the one decision Silent Mode makes: whether an Alarm signals
    with `SoundAndVibration` or `VibrationOnly` (vibration, no sound). `Signalling.of(alarm)`
    and `Signalling.of(silentMode)` are the only mapping. Crucially it is not an
    input to `FiringSession`, `EscalationPolicy`, or the Streak rules, so Silent
    Mode cannot reach dismissal.
  - `AlarmCodec` — the persisted format is now version 2 and carries the Silent
    Mode flag after the enabled flag. Version 1 data still decodes, with Silent
    Mode off, so an existing installed configuration is not lost on upgrade.
- `:app` (thin adapter)
  - `AlarmPlayback` — takes a `Signalling`. `VibrationOnly` starts the `Vibrator`
    but never creates a `MediaPlayer` and never raises the alarm-stream volume, so
    there is no sound path at all. The same `SoundCap` still runs, ending the
    vibration when it expires.
  - `AlarmService` + `AlarmReceiver` — pass the Alarm's Silent Mode through the
    service intent; the service turns it into a `core` `Signalling`.
  - `AlarmListScreen` — the Alarm editor has a "Silent Mode (vibration only)"
    switch, off for a new Alarm; the row shows "Silent Mode · vibration only".
  - `FiringScreen` — shows a Silent Mode notice; the Dismiss Challenge and the
    Physical Anchor controls are unchanged.

How it was verified:

- `./gradlew :core:test :app:assembleDebug --rerun-tasks` green: 179 tests, of
  which 11 are new — `SignallingTest` (4), two `AlarmTest` cases, and five
  `AlarmCodecTest` cases. The codec tests cover the default-off flag, a v2 round
  trip with Silent Mode on, decoding legacy v1 data with Silent Mode off, a
  malformed Silent Mode flag, and a v2 record carrying the legacy field count.
  `SignallingTest` covers the alarm→signalling mapping, the flag default, and that
  Silent Mode does not change when the Alarm fires.
- Dismissal identity is structural: `FiringSession.start` takes no signalling
  input, so Silent Mode has no route into the Dismiss Challenge, Escalation, or
  Streak. Every pre-existing `FiringSession`, `EscalationPolicy`, and `Streak`
  test is unchanged and still green.
- `:app:assembleDebug` green; APK produced.
- CI: [run 35671617611](https://github.com/misaka9981/wake-alarm/actions/runs/35671617611)
  green — core tests, debug APK build, and Release publish all succeeded.

Acceptance:

- "Each Alarm has a Silent Mode toggle, off by default" is covered by
  `Alarm.silentMode`'s default and the editor switch; ticked.
- "Dismissal mechanics, escalation, and Streak rules are identical to a ringing
  Alarm" is guaranteed by construction and by the untouched, still-green core
  tests; ticked.
- The two remaining boxes are the physical outcome on the phone — a Silent Mode
  Alarm actually vibrating with no sound, and the vibration actually stopping at
  the cap. The decision and the adapter wiring are implemented, but neither can
  run in CI, so the boxes are left unchecked.

Pending device confirmation（待机主真机确认）:

- Set an Alarm to Silent Mode, fire it (or use "DEV: Fire Alarm"), and confirm
  the phone vibrates with no sound at all, and that the Dismiss Challenge and
  Physical Anchor still have to be completed to clear it.
- Let a Silent Mode Alarm run past the sound cap and confirm the vibration stops
  on its own while the firing screen stays, saying the Alarm is not cleared.
- Confirm a normal (non-Silent-Mode) Alarm still rings as before.
