# 09: Statistics

**What to build:** A view of how the owner's mornings are going: the current Streak, recent dismissal time and wrong-answer counts, and the average wake time, all derived from recorded history.

**Blocked by:** 05 (An Alarm fires reliably and is dismissed only by Challenge and Anchor), 07 (Escape Hatch)

Status: ready-for-human

- [x] The current Streak is shown.
- [x] Recent dismissal time and wrong-answer counts are shown.
- [x] The average wake time is shown.
- [x] All figures are computed from recorded history rather than transient state.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `WakeStatistics` — the morning statistics as one immutable value:
    `currentStreak`, `recentDismissals`, and `averageWakeTime`. `of(entries, zone)`
    derives all three from the recorded Diagnostic Log (`List<DiagnosticEntry>`),
    so every figure comes from persisted history rather than transient state.
  - `DismissalRecord` — one morning's dismissal: the local `day`, the clock
    `dismissedAt` (the fire instant plus how long the Alarm signalled), the
    `ringingDuration`, and the `wrongAnswers` count.
  - Streak derivation — entries are grouped into local days by `firedAt` in the
    given zone and folded through the existing `Streak`/`DayOutcome` rule: a day
    counts only when **every** firing on it was `Dismissed`, and any `CapExpired`
    or `EscapeHatch` firing breaks it. A day with no recorded firing is not a
    dismissal, so a gap restarts the Streak at one — exactly the rule already
    tested for `Streak`.
  - `recentDismissals` is the most recent `RECENT_DISMISSALS` (7) dismissed
    firings, newest first; firings that were not dismissed have no dismissal time
    and are excluded. `averageWakeTime` is the mean clock time of every recorded
    dismissal (whole-second resolution), or `null` before any dismissal.
  - `StatisticsRepository` — the `core` port named in the spec's "Repository
    ports"; the deciding part ([WakeStatistics.of]) is pure and tested here.
- `:app` (thin adapter)
  - `DataStoreStatistics` — implements the port by reading the Diagnostic Log and
    supplying the device's time zone; no decision of its own.
  - `StatisticsScreen` — the in-app page: "Current Streak", "Average wake time",
    and a "Recent dismissals" list showing each dismissal's day, dismissal clock
    time, how long the Alarm rang, and its wrong-answer count.
  - `AlarmListScreen` / `WakeAlarmRoot` — a "Statistics" entry point next to
    "Diagnostic Log", opening the new screen.

How it was verified:

- `./gradlew :core:test :app:assembleDebug --rerun-tasks` green: 201 core tests,
  of which 13 are new — `StatisticsTest`. They cover an empty log; a single
  dismissal (Streak, the record, and the average); consecutive dismissed days;
  a cap expiry breaking the day; an Escape Hatch breaking the day; a dismissal
  and a cap expiry on the same day still breaking it; the day after a break
  restarting at one; a gap restarting the Streak; averaging two dismissal times
  (06:10 and 07:20 → 06:45); the average ignoring non-dismissed firings; the
  recent list being newest-first and excluding non-dismissed firings; the recent
  list capped at seven; and grouping respecting the supplied time zone. No
  pre-existing test changed.
- `:app:assembleDebug` green; APK produced.

Acceptance:

- All four boxes are covered by the `core` tests and the adapter wiring above, so
  all four are ticked: the Streak, the recent dismissal times and wrong-answer
  counts, and the average wake time are decided in `core` from the recorded log
  and rendered by `StatisticsScreen`.

Pending device confirmation（待机主真机确认）:

- On the phone, fire and dismiss a few Alarms (or use "DEV: Fire Alarm"), then
  open "Statistics" and confirm the Streak, the recent dismissal times with their
  wrong-answer counts, and the average wake time read sensibly, and that they
  still read the same after force-stopping and reopening the app.
- Confirm the numbers match the Diagnostic Log entries (the Statistics page is
  derived from the same recorded history).

