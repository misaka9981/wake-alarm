# 05: An Alarm fires reliably and is dismissed only by Challenge and Anchor

**What to build:** The end-to-end Alarm. At its configured time an Alarm fires on top of the lock screen, rings at a volume that rises to maximum, and vibrates; it keeps signalling until it is dismissed. It can only be dismissed when the Dismiss Challenge is solved **and** the Physical Anchor is reached. Alarms are restored after a device reboot or power loss. The owner is guided to grant the permissions reliability depends on, and warned when one is missing.

**Blocked by:** 02 (Create and persist an Alarm), 03 (Dismiss Challenge), 04 (Physical Anchor)

Status: ready-for-human

- [ ] At the configured time, the Alarm fires over the lock screen without requiring unlock.
- [ ] The Alarm signals with sound rising to maximum volume plus vibration, and does not stop on its own.
- [x] The Alarm is dismissed only when both the Dismiss Challenge is solved and the Physical Anchor is reached; neither alone dismisses it.
- [ ] The Alarm bypasses Do Not Disturb.
- [ ] Alarms are rescheduled after a device reboot and after power loss.
- [ ] The owner is guided to grant exact-alarm, full-screen-intent, battery-optimisation, and Do Not Disturb access, and is warned when any is missing.
- [x] No Snooze affordance appears anywhere in the firing flow.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `FiringSession` — the end-to-end firing state machine. It composes the
    Dismiss Challenge (`DismissalSession`, ticket 03) with the Physical Anchor
    decision (`AnchorScanResult`, ticket 04) and reports `Dismissed` only when
    **both** are satisfied. A correct answer alone, or reaching the anchor alone,
    keeps it `Ringing`; a wrong answer never dismisses even when the anchor is
    already reached. Once the anchor is reached a later wrong scan cannot undo
    it. Events are `AnswerSubmitted`, `Tick`, and `AnchorScanned`; there is no
    skip or Snooze event.
  - `AlarmSchedule` — the next fire instant for an Alarm, strictly after a
    reference time, or `null` when disabled. The scheduling adapter arms the
    platform alarm from this, so the reboot/power-loss re-arming decision is
    tested without a device.
  - `ReliabilityCheck` — decides which reliability guarantees apply at an
    Android API level (exact alarms from API 31, full-screen intents from API
    34, battery optimisation and Do Not Disturb always) and which of those the
    observed `ReliabilityGrants` is missing.
  - `VolumeRamp` — the alarm volume as a function of elapsed ringing time:
    immediately audible, rising monotonically to maximum, never falling.
- `:app` (thin adapter)
  - `AlarmScheduler` — arms exact alarms via `setAlarmClock`, and degrades to
    `setAndAllowWhileIdle` when the exact-alarm grant is missing instead of
    losing the Alarm. `BootReceiver` re-arms every Alarm on `BOOT_COMPLETED` and
    `MY_PACKAGE_REPLACED` (reboot, power loss, app update). The Alarm list
    reschedules on load and after every edit.
  - `AlarmReceiver` + `AlarmService` + `AlarmNotification` + `AlarmPlayback` —
    the firing adapter. The receiver arms the next occurrence, starts a
    media-playback foreground service, and opens the firing screen. The service
    rings on the alarm stream with `VolumeRamp`, vibrates on a repeating
    waveform, and posts an ongoing, non-swipeable full-screen-intent
    notification that bypasses Do Not Disturb when notification policy access is
    granted.
  - `AlarmFiringActivity` + `FiringScreen` — the firing screen, shown over the
    lock screen (`showWhenLocked`/`turnScreenOn`) and kept awake. It renders only
    what `FiringSession` reports: the typed Dismiss Challenge and the Physical
    Anchor scan. When `FiringSession` reports dismissed, it stops the sound and
    vibration and cancels the notification. No Snooze affordance appears.
  - `AndroidReliabilityGrants` + `ReliabilityLauncher` + `ReliabilityScreen` —
    reads the platform grants into the `core` model, opens the right settings
    page per requirement, and guides the owner. The Alarm list shows a warning
    banner when a required guarantee is missing.
  - A `BuildConfig.DEBUG`-only "DEV: Fire Alarm" button on the Alarm list opens
    the end-to-end firing flow without waiting for the configured time; absent
    from release builds.

How it was verified:

- `./gradlew :core:test` green: 115 tests, of which 29 are new —
  `FiringSessionTest` (10), `AlarmScheduleTest` (6), `ReliabilityCheckTest` (7),
  `VolumeRampTest` (6). The firing tests drive event sequences directly: solving
  the challenge alone never dismisses, reaching the anchor alone never dismisses,
  either order of both does, a wrong scan keeps ringing with feedback, an already
  reached anchor is not undone, 20 wrong answers with the anchor reached never
  dismiss, and a dismissed session ignores further events.
- `./gradlew :app:assembleDebug` green; APK produced.
- CI: [run 35666286240](https://github.com/misaka9981/wake-alarm/actions/runs/35666286240)
  green — core tests, debug APK build, and Release publish all succeeded.

Acceptance:

- "Dismissed only when both the Dismiss Challenge is solved and the Physical
  Anchor is reached" and "No Snooze affordance anywhere in the firing flow" are
  covered by the `FiringSession` tests and by code inspection, so those boxes are
  ticked.
- The remaining boxes depend on platform behaviour (lock-screen presentation,
  audio/vibration, DND bypass, real reboot/power loss, and system permission
  dialogs) and can only be confirmed on the phone, so they are left unchecked.

Pending device confirmation（待机主真机确认）:

- Set an Alarm a minute or two ahead and confirm it fires at the configured time,
  appears over the lock screen without unlocking, rings with sound rising to
  maximum plus vibration, and keeps signalling until dismissed.
- Confirm it is dismissed only after solving the typed Dismiss Challenge **and**
  scanning the bound Physical Anchor, and that neither alone silences it.
- Confirm the Alarm is heard with Do Not Disturb on (grant "Do Not Disturb
  access" from the reliability guide first).
- Reboot the phone (and simulate power loss) and confirm the Alarm still fires.
- Open the reliability guide from the list warning and confirm each missing
  guarantee is listed, the "Grant" action opens the right settings page, and the
  warning disappears once granted. Allow notifications when first asked, or the
  full-screen notification cannot appear.
- Tap "DEV: Fire Alarm" on the Alarm list to exercise the flow immediately.
