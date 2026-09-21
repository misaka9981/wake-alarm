# Alarm

A personal Android alarm clock whose purpose is to stop its owner from oversleeping. Unlike a conventional alarm, it cannot be silenced by a half-asleep reflex.

## Language

**Alarm**:
A scheduled event that wakes the owner at a configured time on chosen days of the week.
_Avoid_: Reminder, timer, notification

**Dismiss Challenge**:
The problem the owner must solve before an Alarm is silenced. An Alarm cannot be dismissed without completing its Dismiss Challenge.
_Avoid_: Task, puzzle, question

**Physical Anchor**:
A real-world object or code that the owner must physically reach, away from the bed, as part of dismissing an Alarm.
_Avoid_: QR code, checkpoint, beacon

**Escape Hatch**:
A deliberately effortful last resort that force-silences an Alarm without completing its Dismiss Challenge, reserved for emergencies and defects.
_Avoid_: Skip, override, cancel

**Escalation**:
The increase in Dismiss Challenge difficulty during a single Alarm, driven by elapsed time and wrong answers.
_Avoid_: Difficulty, level

**Streak**:
The count of consecutive days on which the owner dismissed every Alarm without using an Escape Hatch.
_Avoid_: Score, record

**Silent Mode**:
A per-Alarm setting where the Alarm signals by vibration only, with no sound. It changes only how the owner is signalled, never how hard the Alarm is to dismiss.
_Avoid_: Mute, quiet mode

**Diagnostic Log**:
The in-app record of what an Alarm actually did. It exists because the owner has no external logging to inspect.
_Avoid_: Log, telemetry, analytics

**Snooze**:
Absent by design. An Alarm cannot be postponed; the only way past it is the Dismiss Challenge.
_Avoid_: (do not introduce a snooze feature)
