# 04: Physical Anchor

**What to build:** The owner sets a Physical Anchor away from the bed and binds it to an Alarm. During dismissal, the owner must reach the anchor and scan it with the camera; a scan of the wrong anchor fails. Reaching the anchor is decidable in `core` so it can be required alongside the Dismiss Challenge.

**Blocked by:** 01 (Project skeleton, cloud build, and hand-install)

Status: ready-for-agent

- [ ] The owner can set a Physical Anchor and bind it to an Alarm.
- [ ] The scanner accepts the bound anchor and reports success.
- [ ] A scan of anything other than the bound anchor fails and reports failure.
- [ ] Whether the anchor has been reached is decidable in `core` behind a port, with no Android dependency in the decision.
- [ ] The flow is reachable from a development entry point and verifiable on the device.
