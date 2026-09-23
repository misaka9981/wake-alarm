Status: ready-for-agent

> Change note: the Physical Anchor is now optional per Alarm (ADR-0005, ticket
> 17). An Alarm with no bound anchor is dismissed by its Dismiss Challenge alone;
> the anchor stories below describe the behaviour when one is bound.

# Spec: Wake Alarm MVP

## Problem Statement

I oversleep because a conventional phone alarm can be silenced by a reflex performed half-asleep: I reach over, tap snooze or stop, and go back to sleep without ever becoming properly awake. No amount of good intentions survives that moment. I want an alarm whose dismissal genuinely requires me to be awake and out of bed, so that I actually get up.

## Solution

An Android alarm clock that cannot be silenced by reflex. When an Alarm fires, the only way to stop it is to complete a Dismiss Challenge — a multi-step arithmetic problem answered by typing — *and* to physically reach a pre-set Physical Anchor away from the bed. There is no Snooze. The longer the Alarm rings and the more I answer wrong, the harder the challenge becomes. If it rings too long, the sound stops on its own so it cannot trap me or disturb others, but the Alarm stays uncleared and the day's Streak is lost. A deliberately effortful Escape Hatch exists for emergencies and defects only. Because the app is built in the cloud and installed by hand with no device logging, the app keeps its own Diagnostic Log so I can see what an Alarm actually did.

## User Stories

### Scheduling

1. As the owner, I want to create multiple Alarms, so that I can wake at different times on different days.
2. As the owner, I want each Alarm to repeat on chosen days of the week, so that a weekday Alarm does not fire on the weekend.
3. As the owner, I want to set each Alarm's time, so that it matches the daily schedule I need.
4. As the owner, I want to enable and disable an Alarm without deleting it, so that I can pause one temporarily.
5. As the owner, I want to delete an Alarm, so that I can stop it permanently.
6. As the owner, I want my Alarms to survive a device restart, so that a reboot does not make me miss one.
7. As the owner, I want my Alarms to survive the device running out of power, so that a shutdown does not make me miss one.

### Firing reliably

8. As the owner, I want the Alarm to fire at the exact configured time, so that I am not late.
9. As the owner, I want the Alarm to appear on top of the lock screen without unlocking, so that I can act immediately.
10. As the owner, I want the Alarm to bypass Do Not Disturb, so that a silent phone does not hide it.
11. As the owner, I want the Alarm sound to rise to maximum volume, so that I cannot sleep through it.
12. As the owner, I want the Alarm to vibrate as well as ring, so that it is harder to ignore.
13. As the owner, I want the Alarm to keep ringing until I dismiss it (within the sound cap), so that ignoring it is not an option.
14. As the owner, I want to be guided to grant exact-alarm, full-screen, and battery-optimisation permissions, so that reliability is not silently lost.
15. As the owner, I want to be warned when a required permission is missing, so that I know an Alarm may not fire.

### The Dismiss Challenge

16. As the owner, I want to solve a multi-step arithmetic challenge to dismiss an Alarm, so that dismissal requires an awake mind.
17. As the owner, I want to type the answer rather than tap a choice, so that I cannot dismiss it by reflex.
18. As the owner, I want the challenge generated on the device with no network, so that it works whatever the connectivity in the morning.
19. As the owner, I want immediate feedback when my answer is wrong, so that I can try again and know to focus.
20. As the owner, I want a wrong answer to make the next challenge harder, so that careless guessing does not get me out of bed.
21. As the owner, I want each challenge to be verifiably correct, so that a bug cannot make an Alarm impossible to dismiss.

### The Physical Anchor

22. As the owner, I want to set a Physical Anchor away from the bed, so that dismissing an Alarm requires actually leaving the bed.
23. As the owner, I want to bind an Alarm to its anchor, so that the dismissal flow knows which anchor to expect.
24. As the owner, I want to reach the anchor by scanning it with the camera, so that verifying I am physically there needs no extra hardware.
25. As the owner, I want the anchor to be unknown to others, so that nobody can dismiss my Alarm for me.
26. As the owner, I want the challenge and the anchor both to be required, so that neither can be skipped.

### escalation

27. As the owner, I want the challenge difficulty to rise the longer an Alarm rings, so that stalling does not help me.
28. As the owner, I want the difficulty to rise with each wrong answer, so that failure compounds.
29. As the owner, I want to set a default difficulty for each Alarm in advance, so that the morning starts at the level I chose the night before.

### No Snooze

30. As the owner, I want there to be no Snooze button at all, so that the weakest-willpower moment has no easy outlet.

### Silent Mode

31. As the owner, I want to set an Alarm to Silent Mode (vibration only), so that I can wake without disturbing someone else.
32. As the owner, I want a Silent Mode Alarm to keep the same dismissal requirements, so that silencing the sound does not become a shortcut past the challenge.
33. As the owner, I want Silent Mode to be per Alarm and off by default, so that my normal Alarms still ring.

### Sound cap, Streak, and the Escape Hatch

34. As the owner, I want the Alarm sound to stop after a hard time cap, so that a defect or an unanswerable challenge cannot trap me or disturb others forever.
35. As the owner, I want the Alarm to remain uncleared after the cap, so that stopping the noise is not the same as dismissing the Alarm.
36. As the owner, I want the notification to remain until the challenge and anchor are completed, so that I cannot clear it by swiping.
37. As the owner, I want the cap to break the day's Streak, so that letting it ring out has a cost.
38. As the owner, I want an Escape Hatch that requires a hidden long-press and a password, so that it is available in a real emergency but not as a convenience.
39. As the owner, I want usage of the Escape Hatch recorded, so that I can see how often I resort to it.

### Diagnostic Log

40. As the owner, I want an in-app Diagnostic Log of recent Alarms, so that I can see what actually happened without device logging.
41. As the owner, I want the Diagnostic Log to record scheduled time, actual fire time, permission state, volume, challenge duration and wrong answers, and Escape Hatch use, so that any failure is explainable.
42. As the owner, I want the Diagnostic Log readable on the phone, so that I can diagnose a missed Alarm wherever I am.

### Statistics

43. As the owner, I want to see my current Streak, so that I am motivated to keep getting up.
44. As the owner, I want to see my dismissal time and wrong-answer count over recent days, so that I can tell whether I am improving.
45. As the owner, I want to see my average wake time, so that I can judge whether the alarms are well set.

### Onboarding and settings

46. As the owner, I want a first-run setup that sets my wake time and my Physical Anchor, so that the app is usable immediately.
47. As the owner, I want to set the Escape Hatch password during setup, so that only I can use it.
48. As the owner, I want to change settings later, so that my needs can change.

## Implementation Decisions

- **Platform**: native Android in Kotlin with Jetpack Compose. Chosen because the product leans hard on exact alarms, full-screen intents, foreground services, and camera, where native access is the path of least resistance.
- **Testing seam**: a single pure Kotlin `core` module with no Android dependencies, containing every decidable piece of logic. Android ships as thin adapters around it. This is the only seam that automated tests target.
- **Domain vocabulary**: the words in `CONTEXT.md` (Alarm, Dismiss Challenge, Physical Anchor, Escape Hatch, Escalation, Streak, Silent Mode, Diagnostic Log, and the deliberate absence of Snooze) are canonical in code, UI, and tests. Snooze must not be introduced anywhere.
- **DismissalSession**: a pure state machine in `core`. Inputs are events — `AnswerSubmitted`, `AnchorReached`, `Tick`, `EscapeHatchUsed`. Outputs are state transitions — ongoing, dismissed, disabled — plus the data the UI needs. The screen renders only what this machine reports.
- **ChallengeGenerator**: a `core` interface producing a challenge at an integer difficulty and validating a typed answer. The MVP implementation is multi-step arithmetic. The interface exists so other challenge types can be added later without touching the state machine.
- **EscalationPolicy**: a pure `core` function mapping elapsed ringing time and wrong-answer count to the current difficulty fed into the generator.
- **Streak rules**: pure `core` logic that breaks the Streak when a day ends with an uncleared Alarm, an Escape Hatch use, or a sound-cap expiry, and extends it otherwise.
- **Repository ports**: `core` declares interfaces for alarm configuration, anchor configuration, diagnostic events, and statistics. Room (structured history) and DataStore (settings) implement them on the Android side.
- **Alarm scheduling adapter**: exact alarms via the platform alarm API plus a foreground service, rescheduled on boot, app update, and power events.
- **Signalling adapter**: alarm audio stream with a rising-to-maximum volume ramp and vibration, full-screen intent over the lock screen, and a non-dismissable notification while the Alarm is uncleared.
- **Anchor adapter**: camera-based anchor scanning; the anchor is a pre-set code or object. No extra hardware.
- **Silent Mode**: a per-Alarm flag that switches the signalling adapter to vibration only. It does not change DismissalSession, EscalationPolicy, or Streak rules; the sound cap becomes a vibration cap.
- **Escape Hatch**: a hidden long-press plus a password set during onboarding, recorded in the Diagnostic Log.
- **Sound cap**: a fixed cap on continuous signalling; after it, signalling stops but the session stays ongoing.
- **Diagnostic Log**: structured events persisted and rendered in-app, replacing the device logging that cloud-built sideloaded builds cannot provide.
- **Build and distribution**: APK built in GitHub Actions and sideloaded; project lives in a public repo under the `misaka9981` account; CI runs the JVM unit tests. See ADR-0001 and ADR-0004.
- **Signing**: a committed, non-secret debug keystore during the debug phase so CI builds install over one another; a release keystore held in GitHub Secrets when the app stabilises.
- **Onboarding**: first-run flow that sets the wake time, binds the Physical Anchor, and sets the Escape Hatch password.

## Testing Decisions

- **What makes a good test here**: tests exercise the `core` module's external behaviour — given a sequence of events, assert the resulting state and the challenge/difficulty it produces. They never assert on Android framework behaviour, and never on private structure of `core`.
- **Modules tested**: `DismissalSession` (event sequences, dismissal requires both challenge and anchor, Escape Hatch path, cap behaviour), `ChallengeGenerator` (generated challenge is always solvable and its stated answer validates; difficulty changes the challenge), `EscalationPolicy` (elapsed time and wrong answers raise difficulty), and Streak rules (which outcomes break and extend a Streak).
- **Prior art**: none — the repo is greenfield. These tests establish the convention: JVM unit tests run by Gradle in CI, targeting `core` only.
- **Not tested automatically**: scheduling, full-screen intent, audio/vibration, camera anchor scanning, persistence, and the Diagnostic Log UI. These are verified by hand on a real device because CI has no device.

## Out of Scope

- Accounts, sign-in, cloud sync, and cross-device sync.
- LLM-generated or online challenge types, and richer offline types such as memory or logic challenges.
- Social or financial consequences for missing an Alarm.
- Publishing to an app store; iOS; widgets; wearables; complex calendar-style scheduling.
- Automated instrumented or emulator tests in CI.
- A statistics UI beyond the basic numbers listed above.

## Further Notes

- The build-and-test loop is slow (cloud build, hand install, no logcat), so changes should be batched and verified deliberately; the Diagnostic Log is the primary feedback channel.
- Android 14+ restricts full-screen intents and Android 12+ gates exact alarms; the app must degrade gracefully and tell the owner when a guarantee cannot be met.
- The Escape Hatch is a genuine tension: it exists for defects and emergencies, yet a half-asleep owner will reach for it. It is deliberately effortful, and its use is recorded.
- Silent Mode is a real weakening of the anti-oversleep mechanism and is therefore opt-in per Alarm, never the default.
