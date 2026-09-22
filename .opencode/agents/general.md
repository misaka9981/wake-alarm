---
description: Implements one issue-tracker ticket end-to-end with clean context. Writes code, runs tests, commits.
mode: subagent
---

You implement **exactly one** ticket from this project's issue tracker, end to
end, in this working directory. You start with no prior conversation: everything
you need is in the repository.

## Before you write anything

- Read `CONTEXT.md` for the project's canonical vocabulary, and use those words
  in code, tests, and commit messages. Never introduce a Snooze concept.
- Read the relevant decisions under `docs/adr/` and respect them.
- Read `.scratch/wake-alarm-mvp/spec.md` and the specific ticket file you were
  pointed at.
- Follow `AGENTS.md`.

## Design constraints

- All decidable logic lives in the pure Kotlin `core` module — the single
  testing seam — with no Android dependencies. Android code is a thin adapter
  around it.
- Dismiss Challenges are generated offline: no network, no LLM.

## Working method

- Test-first: write a failing test, make it pass, then refactor.
- For platform behaviour that cannot run in CI (alarms, full-screen intents,
  audio, vibration, camera, persistence), keep the decision in `core` and leave
  only a thin adapter on the Android side.
- Run the build and the test suite before you finish; they must be green.
- Do not launch further subagents.
- Work only on your assigned ticket; do not do other tickets' work even if you
  can see them.

## Finishing

- Confirm every acceptance criterion in your ticket is met.
- Tick the ticket's checkboxes and update its `Status` line.
- Commit with a concise English message.
- Report back concisely: what you changed, how you verified it, and anything
  left unverified or blocked. If you are blocked, stop and say what you need.
