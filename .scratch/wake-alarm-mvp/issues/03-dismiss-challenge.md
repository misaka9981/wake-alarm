# 03: Dismiss Challenge

**What to build:** The Dismiss Challenge, reachable from a development entry point. The owner is shown a multi-step arithmetic problem and must type the answer. A correct answer ends the session as dismissed; a wrong answer gives feedback and makes the next problem harder. The longer the session runs and the more answers are wrong, the higher the difficulty becomes. There is no way to skip or postpone.

**Blocked by:** 01 (Project skeleton, cloud build, and hand-install)

Status: ready-for-agent

- [ ] A Dismiss Challenge is a multi-step arithmetic problem answered by typing.
- [ ] A correct answer transitions the session to dismissed.
- [ ] A wrong answer gives immediate feedback and raises the difficulty of subsequent challenges.
- [ ] Difficulty is an input to challenge generation, and elapsed session time raises it independently of wrong answers.
- [ ] Challenge generation is offline and always yields a problem whose stated answer validates.
- [ ] The session state machine, the challenge generator, and the escalation policy live in `core` and are covered by JVM unit tests over event sequences.
- [ ] No Snooze path exists anywhere.
- [ ] The flow is reachable from a development entry point and verifiable on the device.
