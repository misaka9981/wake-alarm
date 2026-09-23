# 17: Optional Physical Anchor and a Silent Mode that actually vibrates

**What to build:** Make the QR-scan (Physical Anchor) flow optional per Alarm, so an Alarm with no bound anchor is dismissed by its Dismiss Challenge alone; and make a Silent Mode Alarm actually vibrate when the phone is in Silent Mode or Do Not Disturb.

**Blocked by:** 05 (An Alarm fires reliably and is dismissed only by Challenge and Anchor), 11 (Silent Mode per Alarm)

Status: ready-for-human

- [x] An Alarm with no bound Physical Anchor is dismissed by its Dismiss Challenge alone.
- [x] An Alarm with a bound Physical Anchor still requires the scan.
- [x] First-run setup no longer forces an anchor; it is set later from settings if wanted.
- [x] The firing screen shows the anchor section only when one is bound.
- [ ] A Silent Mode Alarm actually vibrates with no sound, including under Silent Mode / Do Not Disturb.
- [ ] The vibration still stops at the sound cap.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `FiringSession` now takes `anchorRequired`. `settle` dismisses when the
    Dismiss Challenge is solved and, only when `anchorRequired`, the anchor is
    also reached. `start(..., anchorRequired = true)` preserves the previous
    behaviour by default; an Alarm with no bound anchor passes `false`.
  - `Onboarding` no longer consults anchors: `isComplete` is `firstAlarm != null
    && passwordSet`, `missingSteps` has only `WakeTime` and
    `EscapeHatchPassword`, and `OnboardingStep.PhysicalAnchor` is gone.
  - `CONTEXT.md`'s Physical Anchor entry now reads as optional; ADR-0003 is
    superseded by the new ADR-0005.
- `:app` (thin adapter)
  - `AlarmFiringActivity` passes `anchorRequired = catalog.anchorFor(id) != null`.
  - `FiringScreen` renders the anchor section only when `anchorLabel != null`.
  - `OnboardingScreen` / `WakeAlarmRoot` drop the anchor step and its camera
    wiring from first-run setup. Settings still offers the optional anchor.
  - `AlarmPlayback` tags its vibration as alarm usage — `VibrationAttributes
    .USAGE_ALARM` on API 33+, `AudioAttributes.USAGE_ALARM` below. A plain
    vibration defaults to an unknown usage, which the platform suppresses under
    Silent Mode / Do Not Disturb; that is why a Silent Mode Alarm signalled
    nothing.
  - Strings (both locales) no longer say the anchor is mandatory.

How it was verified:

- `./gradlew :core:test :app:assembleDebug` green: 227 tests, of which 3 are new
  — two `FiringSessionTest` cases (`whenNoAnchorIsBoundTheChallengeAloneDismisses`,
  `whenNoAnchorIsBoundAReachedAnchorDoesNotDismissByItself`) and a reworked
  `OnboardingTest` (`wakeTimeAndPasswordCompleteOnboardingWithoutAnAnchor`).
  `FiringSessionTest` (29) and `OnboardingTest` (6) both pass, 0 failures.
- The `:app:assembleDebug` build produces `app/build/outputs/apk/debug/app-debug.apk`.
- CI: pending push.

Acceptance:

- The three logic boxes are covered by the `core` tests and code inspection and
  are ticked.
- The two vibration boxes are physical outcomes on the phone and cannot run in
  CI, so they are left unchecked.

Pending device confirmation（待机主真机确认）:

- Fire an Alarm that has no bound anchor and confirm solving the Dismiss
  Challenge alone dismisses it.
- Fire an Alarm with a bound anchor and confirm the scan is still required.
- Set an Alarm to Silent Mode, fire it, and confirm the phone vibrates with no
  sound at all, including with Silent Mode / Do Not Disturb on.
- Let a Silent Mode Alarm run past the sound cap and confirm the vibration stops
  on its own while the firing screen stays.
