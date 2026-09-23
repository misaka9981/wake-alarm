# Make the Physical Anchor optional per Alarm

**Status**: accepted

Supersedes ADR-0003 (require a Physical Anchor to dismiss).

The owner asked for the QR-scan flow to be optional: requiring an anchor for
every Alarm meant every Alarm needed its own code to scan, and an Alarm with no
bound anchor could never be dismissed at all. The core anti-oversleep mechanism —
the typed Dismiss Challenge that escalates with time and wrong answers — is the
part that must work now, so the Physical Anchor becomes an optional extra the
owner may bind to an Alarm rather than a mandatory part of every Alarm.

## Consequences

- `FiringSession` takes an `anchorRequired` flag. It is `true` when the Alarm has
  a bound anchor and `false` otherwise; an Alarm with no bound anchor is
  dismissed by its Dismiss Challenge alone.
- Onboarding no longer asks for an anchor: setup is complete once the wake time
  and the Escape Hatch password are set. The owner can still bind an anchor to
  their primary Alarm from settings.
- The firing screen shows the anchor section only when one is bound.
- The Escape Hatch, Escalation, the sound cap, and the Streak rules are
  unchanged. Silent Mode still changes only how the owner is signalled.
- An Alarm with no anchor is weaker than the original design intended: it can be
  dismissed from bed. That is an accepted trade-off for now, revisited if the
  challenge alone proves insufficient.
