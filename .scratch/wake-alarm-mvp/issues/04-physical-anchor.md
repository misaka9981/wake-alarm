# 04: Physical Anchor

**What to build:** The owner sets a Physical Anchor away from the bed and binds it to an Alarm. During dismissal, the owner must reach the anchor and scan it with the camera; a scan of the wrong anchor fails. Reaching the anchor is decidable in `core` so it can be required alongside the Dismiss Challenge.

**Blocked by:** 01 (Project skeleton, cloud build, and hand-install)

Status: ready-for-human

- [x] The owner can set a Physical Anchor and bind it to an Alarm.
- [x] The scanner accepts the bound anchor and reports success.
- [x] A scan of anything other than the bound anchor fails and reports failure.
- [x] Whether the anchor has been reached is decidable in `core` behind a port, with no Android dependency in the decision.
- [ ] The flow is reachable from a development entry point and verifiable on the device.

## Comments

Implemented on branch `main`.

What was added:

- `:core` (pure Kotlin, the only testing seam)
  - `PhysicalAnchor` / `AnchorCode` — a real-world object the owner places away
    from the bed. Its code is whatever the object encodes (a printed QR code, a
    product barcode, anything the camera reads) and is the anchor's identity, so
    re-scanning the same object relabels one anchor instead of creating a second.
  - `AnchorCatalog` — the set/bind/unbind/delete rules over an immutable catalog,
    keyed by code and ordered by label. A binding must point at a set anchor, so
    a dangling binding can never exist. `anchorFor` / `verifierFor` / `gateFor`
    expose the per-Alarm anchor the dismissal flow will consult.
  - `AnchorScanner` — the port `core` declares for reading an anchor; the Android
    adapter implements it with the camera. It returns the raw payload only.
  - `AnchorVerifier` / `AnchorGate` — the decision. Reached only when the scanned
    payload is exactly the bound anchor's code; every other payload, a cancelled
    scan, and an Alarm with no bound anchor are `NotReached`. This is pure
    comparison logic with no Android dependency.
  - `AnchorCodec` — versioned encode/decode of anchors plus bindings. Fields are
    Base64-encoded because a scanned code is arbitrary real-world payload and may
    contain the separator or a newline.
  - `AnchorRepository` — the anchor persistence port.
- `:app` (thin adapter)
  - `DataStoreAnchorRepository` — persists the codec's string in its own
    Preferences DataStore; unreadable data falls back to empty.
  - `AndroidAnchorScanner` — the camera port, built on the Google code-scanner
    API. It makes no judgement about the payload.
  - `AnchorScreen` — the development entry point: pick an Alarm, "Set Physical
    Anchor from scan" (scan an object, then set and bind it to the Alarm), and
    "Reach Anchor from scan" (scan again; the screen shows "Anchor reached." or
    "Not the bound Physical Anchor."). Rendering only; every decision comes from
    `core`. Opened from a `BuildConfig.DEBUG`-only "DEV: Physical Anchor" button
    on the Alarm list, and absent from release builds.

Design note: the MVP anchor is "a pre-set code or object" (spec, Implementation
Decisions). The owner sets it by scanning a real object once; the app does not
generate or print a code of its own. This keeps the flow offline, needs no extra
hardware, and needs no QR-rendering dependency.

How it was verified:

- `./gradlew :core:test` green: 86 tests, of which 38 are new —
  `PhysicalAnchorTest` (4), `AnchorCatalogTest` (16), `AnchorReachTest` (8),
  `AnchorCodecTest` (10). They drive the acceptance directly: an anchor can be
  set and bound to an Alarm; a scan of the bound code is `Reached`; a scan of any
  other code, a cancelled scan, and an Alarm with no bound anchor are
  `NotReached`; `AnchorGate` reaches those decisions through an injected fake
  `AnchorScanner` port, so the decision has no Android dependency. The codec
  tests round-trip anchors, bindings, and awkward payloads, and reject malformed
  or dangling data.
- `./gradlew :app:assembleDebug` green; APK produced.
- CI: see the run linked from the commit.

Acceptance:

- The four logic boxes are covered by the `core` tests above and are ticked.
- The development entry point is implemented and builds, but confirming it on the
  phone needs the device, so that box is left unchecked.

Pending device confirmation（待机主真机确认）:

- Tap "DEV: Physical Anchor", pick an Alarm, tap "Set Physical Anchor from scan",
  and scan a real object away from the bed.
- Tap "Reach Anchor from scan" and scan the same object: it must report "Anchor
  reached.".
- Scan a different object (or cancel): it must report "Not the bound Physical
  Anchor." / "Scan cancelled.".
- Requires Google Play services for the camera scanner; this is unverified.
