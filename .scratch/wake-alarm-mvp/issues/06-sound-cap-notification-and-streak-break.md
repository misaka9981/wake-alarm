# 06: Sound cap, non-dismissable notification, and Streak break

**What to build:** A safety valve that stops an Alarm from ringing forever without letting the owner off the hook. After a fixed cap of continuous signalling, the sound stops automatically, but the Alarm stays uncleared and its notification cannot be swiped away until the challenge and anchor are completed. Letting the cap expire breaks the day's Streak.

**Blocked by:** 05 (An Alarm fires reliably and is dismissed only by Challenge and Anchor)

Status: ready-for-human

- [ ] After a fixed cap of continuous signalling, sound and vibration stop automatically.
- [x] After the cap, the Alarm remains in the ongoing, uncleared state.
- [ ] The notification persists and cannot be dismissed until the Dismiss Challenge and Physical Anchor are completed.
- [x] A cap expiry breaks the day's Streak.
- [x] Completing challenge and anchor after the cap still clears the Alarm.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `SoundCap` — the fixed cap on continuous signalling. `hasExpired(elapsed)`
    is false before the cap and true at and after it. The default is ten
    minutes: long enough to wake the owner and finish the Dismiss Challenge,
    short enough that a defect does not ring forever. In Silent Mode the same
    cap ends vibration instead of sound; it never changes how hard the Alarm is
    to dismiss.
  - `FiringSession` — now applies the cap to the elapsed ringing time. A `Tick`
    that reaches the cap sets a new `FiringState.Ringing.capExpired` flag, but
    the state stays `Ringing` and the Dismiss Challenge keeps escalating: the
    Alarm is uncleared, and solving the challenge and reaching the anchor after
    the cap still transitions to `Dismissed`. Ringing time only grows, so a
    backwards tick cannot resurrect sound the cap has stopped.
  - `Streak` / `DayOutcome` — the pure Streak rule. A `DayOutcome.CapExpired`
    breaks the Streak to zero even when an earlier Alarm on the same day was
    dismissed, so `Dismissed` afterwards cannot buy the day back; the next
    dismissed day restarts the Streak at one. (Recording the day's outcome and
    displaying the Streak belong to the Diagnostic Log and Statistics tickets.)
- `:app` (thin adapter)
  - `AlarmPlayback` — applies `SoundCap` on its periodic tick; when the cap
    expires it releases the `MediaPlayer` and cancels vibration but the adapter
    stays alive so its owner keeps the notification.
  - `AlarmService` — a new `stopSignalling` command stops the sound and
    vibration without stopping the service, so the ongoing, non-swipeable,
    full-screen-intent notification persists after the cap; the service is only
    stopped when `FiringSession` reports dismissed. The playback also applies
    the cap itself, so the noise stops even if the firing screen is not present.
  - `AlarmFiringActivity` + `FiringScreen` — the activity tells the service to
    stop signalling when the session reports `capExpired`, and the screen shows
    that signalling stopped while the Alarm remains uncleared.

How it was verified:

- `./gradlew :core:test --rerun-tasks` green: 135 tests, of which 20 are new —
  `SoundCapTest` (7), `StreakTest` (8), and five new `FiringSessionTest` cases.
  The firing tests drive event sequences: signalling continues before the cap,
  `capExpired` flips exactly at the cap and is not undone by a backwards tick,
  the session stays `Ringing` and uncleared after the cap, and solving the typed
  challenge then reaching the anchor after the cap still reports `Dismissed`.
  The Streak tests cover extension over consecutive days, a cap expiry breaking
  the Streak (including after an earlier same-day dismissal, and after a
  post-cap completion), the restart at one the next day, a missed day, and
  same-day double counting.
- `./gradlew :app:assembleDebug` green; APK produced.

Acceptance:

- "After the cap, the Alarm remains in the ongoing, uncleared state", "A cap
  expiry breaks the day's Streak", and "Completing challenge and anchor after
  the cap still clears the Alarm" are covered by the `core` tests above, so
  those boxes are ticked.
- The other two boxes are the physical outcome on the phone — the sound and
  vibration actually stopping, and the ongoing notification resisting a swipe
  attempt until the challenge and anchor are done. The decision and the adapter
  wiring are implemented, but neither can run in CI, so the boxes are left
  unchecked.

Pending device confirmation（待机主真机确认）:

- Tap "DEV: Fire Alarm", let it ring past the cap, and confirm the sound and
  vibration stop on their own while the firing screen stays, saying the Alarm
  is not cleared.
- After the cap, swipe the notification away and confirm it cannot be dismissed;
  then solve the Dismiss Challenge and scan the bound Physical Anchor and
  confirm the Alarm clears and the notification goes away.
