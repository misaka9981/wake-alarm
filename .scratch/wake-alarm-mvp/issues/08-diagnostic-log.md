# 08: Diagnostic Log

**What to build:** Because cloud-built sideloaded builds have no device logging, the app keeps its own record of what each Alarm actually did and shows it in a page the owner can read on the phone. Any missed or misbehaving Alarm becomes explainable.

**Blocked by:** 05 (An Alarm fires reliably and is dismissed only by Challenge and Anchor)

Status: ready-for-agent

- [ ] The log records, per Alarm firing: scheduled time, actual fire time, permission state at fire, signalling volume, challenge duration, wrong-answer count, and whether the Escape Hatch was used.
- [ ] Log entries are persisted and survive an app restart.
- [ ] The log is readable in-app on the phone.
- [ ] The page shows a bounded recent history rather than growing without limit.
