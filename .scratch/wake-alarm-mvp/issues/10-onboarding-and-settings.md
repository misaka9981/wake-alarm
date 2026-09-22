# 10: First-run onboarding and settings

**What to build:** A first-run flow that makes the app usable immediately — set the wake time, bind the Physical Anchor, and set the Escape Hatch password — plus settings for changing these later.

**Blocked by:** 02 (Create and persist an Alarm), 04 (Physical Anchor), 07 (Escape Hatch)

Status: ready-for-human

- [x] First run sets the wake time and creates the first Alarm.
- [x] First run binds the Physical Anchor.
- [x] First run sets the Escape Hatch password.
- [x] The owner can change the wake time, the anchor, and the password later in settings.
- [x] The app is usable for a full night's sleep immediately after onboarding.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `Onboarding` — the first-run decision, derived from the configuration that
    actually exists rather than from a separate flag: the app is usable exactly
    when an Alarm exists, a Physical Anchor is bound to it, and the Escape Hatch
    password is set (`isComplete`), so finishing setup cannot drift from what the
    dismissal flow will later require. It reports which `OnboardingStep`s remain
    (`missingSteps`) and picks the earliest Alarm as the one settings changes the
    wake time of. `Onboarding.firstAlarm(time)` is the first Alarm the flow
    creates: enabled and repeating every day, so it fires on the very next day
    whichever day that is, rings by default (Silent Mode off), and starts at the
    default difficulty — i.e. the app is usable with no further tuning.
- `:app` (thin adapter)
  - `OnboardingScreen` — the first-run flow. It resumes at
    `Onboarding.missingSteps.first()`: set the wake time (creating the first
    Alarm and arming it through the scheduler), scan and bind a Physical Anchor,
    then set and confirm the Escape Hatch password. It renders only; every rule
    comes from `core`.
  - `SettingsScreen` — reachable from the Alarm list ("Settings") in every build.
    It changes the wake time (reuses the Alarm editor for the earliest Alarm,
    keeping its id so the anchor binding survives), rebinds the Physical Anchor
    from a scan, and changes the Escape Hatch password.
  - `WakeAlarmRoot` now loads `Onboarding` from the repositories before showing
    anything: until setup is complete the onboarding flow is shown and the rest
    of the app is withheld, so an installed app cannot be mistaken for a usable
    one. `MainActivity` wires the existing `DataStoreEscapeHatchRepository` in.
  - `AlarmListScreen`/`AnchorScreen` — a "Settings" entry point was added and the
    Alarm editor and anchor-label helper were opened up (`internal`) for reuse.

How it was verified:

- `./gradlew :core:test` green: 209 tests, of which 8 are new —
  `OnboardingTest` (8). They drive the acceptance directly: the first Alarm is
  enabled, repeats every day, rings, and starts at the default difficulty;
  nothing set yet leaves all three steps (`WakeTime`, `PhysicalAnchor`,
  `EscapeHatchPassword`); after the wake time the anchor and password remain;
  after an anchor bound to that Alarm only the password remains; all three
  together complete setup; an anchor bound to another Alarm does not count; the
  earliest Alarm is the one shown; and a disabled first Alarm still counts as
  set up (so disabling it later does not re-open onboarding).
- `./gradlew :app:assembleDebug` green; APK produced.

Acceptance:

- All five boxes are backed by the `core` tests above plus the wired adapters:
  the wake time creates the first Alarm (`Onboarding.firstAlarm`), the anchor
  binding and password presence are the `anchorBound`/`passwordSet` decisions,
  settings calls the same tested `AlarmCatalog`/`AnchorCatalog`/
  `EscapeHatchRepository` operations, and `isComplete` is exactly the
  "usable for a full night's sleep" condition. The UI itself is a thin renderer
  over those decisions and builds.
- Onboarding completion is derived rather than stored, by design: the first-run
  flow reappears only if the configuration that makes the app usable is gone
  (for example the owner deletes every Alarm), which matches the intent.

Pending device confirmation（待机主真机确认）:

- Install the APK on a fresh install and walk the first-run flow: set a wake
  time and confirm the Alarm appears on the list afterwards.
- Bind a Physical Anchor by scanning a real object away from the bed (requires
  Google Play services for the camera scanner; the scan itself is unverified).
- Set the Escape Hatch password and confirm setup then gives way to the Alarm
  list.
- Open Settings and change the wake time, the anchor, and the password, and
  confirm each change sticks after leaving and re-entering the screen.
- Confirm the full first night works end to end (this overlaps ticket 05's
  pending device checks).
