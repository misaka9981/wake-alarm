# 05: An Alarm fires reliably and is dismissed only by Challenge and Anchor

**What to build:** The end-to-end Alarm. At its configured time an Alarm fires on top of the lock screen, rings at a volume that rises to maximum, and vibrates; it keeps signalling until it is dismissed. It can only be dismissed when the Dismiss Challenge is solved **and** the Physical Anchor is reached. Alarms are restored after a device reboot or power loss. The owner is guided to grant the permissions reliability depends on, and warned when one is missing.

**Blocked by:** 02 (Create and persist an Alarm), 03 (Dismiss Challenge), 04 (Physical Anchor)

Status: ready-for-agent

- [ ] At the configured time, the Alarm fires over the lock screen without requiring unlock.
- [ ] The Alarm signals with sound rising to maximum volume plus vibration, and does not stop on its own.
- [ ] The Alarm is dismissed only when both the Dismiss Challenge is solved and the Physical Anchor is reached; neither alone dismisses it.
- [ ] The Alarm bypasses Do Not Disturb.
- [ ] Alarms are rescheduled after a device reboot and after power loss.
- [ ] The owner is guided to grant exact-alarm, full-screen-intent, battery-optimisation, and Do Not Disturb access, and is warned when any is missing.
- [ ] No Snooze affordance appears anywhere in the firing flow.
