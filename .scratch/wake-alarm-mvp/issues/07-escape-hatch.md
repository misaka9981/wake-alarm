# 07: Escape Hatch

**What to build:** A deliberately effortful last resort for emergencies and defects. By long-pressing a hidden target and entering a private password, the owner force-silences an Alarm without completing the challenge or the anchor. Each use is recorded and breaks the day's Streak. It is not a visible button and not a normal path out.

**Blocked by:** 05 (An Alarm fires reliably and is dismissed only by Challenge and Anchor)

Status: ready-for-human

- [x] A hidden long-press plus the correct password force-silences an Alarm without completing the Dismiss Challenge or the Physical Anchor.
- [x] A wrong password does not force-silence the Alarm.
- [x] The affordance is hidden and requires both gestures, so it cannot be triggered by reflex.
- [x] Each use is recorded for later display and for statistics.
- [x] Each use breaks the day's Streak.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `EscapeHatch` + `EscapeHatchPassword` — the owner's private password and
    the decision `unlocks(submitted)`. A blank password cannot be constructed,
    and an Alarm with no configured password (`EscapeHatch.none`) fails closed:
    nothing unlocks. The gesture and the password entry stay on the Android side.
  - `FiringSession` — now owns the Escape Hatch. `EscapeHatchLongPressed`
    reveals the prompt; `EscapeHatchPasswordSubmitted(password)` reaches
    `FiringState.EscapeHatchUsed` **only** when the prompt was already revealed by
    the long-press *and* the password is correct. That terminal state
    force-silences the Alarm without the Dismiss Challenge or the Physical Anchor,
    and it works even after the sound cap. A wrong password, a password with no
    prior long-press, or a password when none is configured all leave the Alarm
    `Ringing`. `EscapeHatchCancelled` hides the prompt again.
  - `EscapeHatchUse` + `EscapeHatchLog` — the recorded fact of a use (Alarm id +
    instant) and the `core` port that persists it, so the Diagnostic Log (ticket
    08) can show how often the Escape Hatch is used and Statistics (ticket 09)
    can derive the day from it.
  - `EscapeHatchUseCodec` — the decidable part of that persistence (versioned,
    Base64-encoded Alarm id, ISO-8601 instant); tested.
  - `Streak`/`DayOutcome` — a new `DayOutcome.EscapeHatch` breaks the Streak to
    zero, exactly as a cap expiry does, even when an earlier Alarm on the same
    day was dismissed; the next dismissed day restarts it at one.
- `:app` (thin adapter)
  - `DataStoreEscapeHatchRepository` — persists the password (ticket 10 will let
    the owner set it); a blank stored value is treated as "no password set".
  - `DataStoreEscapeHatchLog` — appends each use via `EscapeHatchUseCodec`.
  - `FiringScreen` — the Escape Hatch is not a visible button: a long-press on
    the Alarm title reveals the password prompt (masked field, "Force-silence"
    and "Cancel"). Rendering only; `core` decides whether both gestures and the
    password are satisfied.
  - `AlarmFiringActivity` — loads the password into the `FiringSession`; when it
    reports `EscapeHatchUsed`, records an `EscapeHatchUse` and then stops the
    Alarm (the record runs first, so the use survives even though the screen
    closes). A brief "Escape Hatch used. … the day's Streak is broken." screen is
    shown.

How it was verified:

- `./gradlew :core:test --rerun-tasks` green: 155 tests, of which 20 are new —
  `EscapeHatchTest` (4), `EscapeHatchUseCodecTest` (4), nine new
  `FiringSessionTest` cases, and three new `StreakTest` cases. The firing tests
  drive event sequences: a password without the long-press does nothing, the
  long-press alone does nothing, long-press + correct password force-silences
  without the challenge or anchor (also after the cap), a wrong password keeps
  the Alarm ringing and uncleared, no configured password never unlocks, an
  Escape Hatch use is terminal, a cancelled prompt must be re-revealed, and an
  already-dismissed Alarm cannot be force-silenced.
- `./gradlew :app:assembleDebug` green; APK produced.

Acceptance:

- All five boxes are covered by `core` tests and by code inspection: the
  force-silence bypasses the challenge and anchor, a wrong password does not
  silence, the two-gesture requirement is enforced in `core` and the affordance
  is a hidden long-press rather than a button, every use is recorded through
  `EscapeHatchLog`/`EscapeHatchUseCodec`, and `DayOutcome.EscapeHatch` breaks the
  day's Streak.

Pending device confirmation（待机主真机确认）:

- The physical outcome — that the ringing sound and vibration actually stop when
  the long-press + password is used, and that the hidden long-press works on
  screen — needs the phone.
- The password must be set first, which is ticket 10's job (first-run
  onboarding); until then the Alarm correctly fails closed and cannot be
  force-silenced.
