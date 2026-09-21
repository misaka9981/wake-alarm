# 03: Dismiss Challenge

**What to build:** The Dismiss Challenge, reachable from a development entry point. The owner is shown a multi-step arithmetic problem and must type the answer. A correct answer ends the session as dismissed; a wrong answer gives feedback and makes the next problem harder. The longer the session runs and the more answers are wrong, the higher the difficulty becomes. There is no way to skip or postpone.

**Blocked by:** 01 (Project skeleton, cloud build, and hand-install)

Status: ready-for-human

- [x] A Dismiss Challenge is a multi-step arithmetic problem answered by typing.
- [x] A correct answer transitions the session to dismissed.
- [x] A wrong answer gives immediate feedback and raises the difficulty of subsequent challenges.
- [x] Difficulty is an input to challenge generation, and elapsed session time raises it independently of wrong answers.
- [x] Challenge generation is offline and always yields a problem whose stated answer validates.
- [x] The session state machine, the challenge generator, and the escalation policy live in `core` and are covered by JVM unit tests over event sequences.
- [x] No Snooze path exists anywhere.
- [ ] The flow is reachable from a development entry point and verifiable on the device.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `Challenge` / `ChallengeGenerator` — a problem with its stated answer, and
    the interface that generates one at an integer difficulty and validates a
    typed answer. `ArithmeticChallengeGenerator` is the MVP implementation: a
    multi-step arithmetic problem generated on the device with no network. Higher
    difficulty means more operations and larger operands; the running value never
    drops below 1, so every answer is a plain non-negative integer.
  - `EscalationPolicy` — a pure function mapping the per-Alarm base difficulty,
    elapsed session time, and wrong-answer count to the difficulty fed into the
    generator. Time and wrong answers raise it independently and compound, capped
    at a maximum.
  - `DismissalSession` — the event-driven state machine (`AnswerSubmitted`,
    `Tick`). A correct answer transitions to `Dismissed`; a wrong answer gives
    feedback and regenerates a harder challenge; a `Tick` that crosses an
    escalation step also regenerates a harder challenge, so stalling does not
    help. There is no skip or postpone event: no Snooze path exists.
- `:app` (thin adapter)
  - `DismissChallengeScreen` — renders what `core` reports and owns only the
    clock (driving `Tick`) and the text input.
  - `WakeAlarmRoot` + a `BuildConfig.DEBUG`-only button on the Alarm list — the
    development entry point that opens the Dismiss Challenge without an Alarm
    ringing. It is absent from release builds.

How it was verified:

- `./gradlew :core:test` green: 48 tests, of which 26 are new —
  `ArithmeticChallengeGeneratorTest` (9), `DismissalSessionTest` (10),
  `EscalationPolicyTest` (7). The generator tests assert that every generated
  challenge validates its own stated answer across difficulties 1–12 over 250
  samples each, that a wrong or non-numeric answer does not validate, and that
  higher difficulty adds operations. The session tests drive event sequences:
  correct ⇒ dismissed, wrong ⇒ ongoing + feedback + harder, elapsed ⇒ harder
  independently, dismissed ⇒ inert, and no sequence of wrong answers dismisses.
- `./gradlew :app:assembleDebug` green.
- CI: [run 35662949683](https://github.com/misaka9981/wake-alarm/actions/runs/35662949683)
  green — core tests, debug APK build, and Release publish all succeeded.

Acceptance:

- The seven logic boxes are covered by the `core` tests above and are ticked.
- The development entry point is implemented and builds; confirming it on the
  phone needs the device, so that box is left unchecked.

Pending device confirmation（待机主真机确认）:

- Tap "DEV: Dismiss Challenge" on the Alarm list, solve the typed arithmetic
  problem, and confirm a correct answer ends the flow while a wrong answer shows
  feedback and produces a harder next problem.

