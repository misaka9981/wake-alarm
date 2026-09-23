# Require a Physical Anchor to dismiss

**Status**: superseded by ADR-0005

> This decision was reversed by ADR-0005: the Physical Anchor is now optional per
> Alarm. The record below is kept for history.

A Dismiss Challenge that can be solved while lying in bed is solvable half-asleep and is regularly followed by more sleep, which defeats the purpose. Dismissing an Alarm therefore requires physically reaching a pre-set Physical Anchor — a QR code or object the owner places away from the bed — in addition to completing the Dismiss Challenge. The anchor is chosen by the owner and unknown to anyone who might try to dismiss the Alarm for them.

## Consequences

- The owner must set up an anchor during onboarding, and the anchor's location becomes part of what an Alarm is configured with.
- An Alarm's sound cannot be stopped by the challenge alone; the dismissal flow must sequence the challenge and the anchor, and neither may be skipped.
