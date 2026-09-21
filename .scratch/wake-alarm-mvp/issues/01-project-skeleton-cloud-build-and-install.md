# 01: Project skeleton, cloud build, and hand-install

**What to build:** The repository and pipeline exist, and a build reaches the phone. A pushed change produces an installable APK that the owner downloads from the phone browser and opens, launching to a placeholder screen. This establishes the only testing seam the project will use.

**Blocked by:** None (can start immediately)

Status: ready-for-human

- [x] The repository is initialised and pushed to a public GitHub repo under the `misaka9981` account, and CI runs on push.
- [x] A Kotlin + Jetpack Compose app and a pure `core` Kotlin module with no Android dependencies both exist.
- [x] A trivial `core` unit test exists and passes in CI.
- [x] CI runs the `core` module's JVM unit tests and builds a debug-signed APK using a committed, non-secret debug keystore.
- [x] CI publishes the APK to a GitHub Release.
- [ ] The APK installs by hand from the phone browser and launches to a placeholder screen.

## Comments

Implemented on branch `main`, pushed to https://github.com/misaka9981/wake-alarm.

What exists now:

- `:core` — pure Kotlin/JVM module, no Android dependencies. The single test seam.
- `:app` — thin Jetpack Compose adapter; the placeholder screen renders `AppInfo.NAME` from `:core`, proving the adapter seam.
- `.github/workflows/ci.yml` — on push to `main`: runs `:core:test`, builds `:app:assembleDebug`, and publishes the signed APK to a GitHub Release tagged `build-<run_number>`.
- `keystore/debug.keystore` — committed, non-secret debug keystore so the debug signature is stable across CI runs (see ADR-0001).

Evidence:

- CI run [35659876770](https://github.com/misaka9981/wake-alarm/actions/runs/35659876770) green: "Core JVM unit tests", "Build debug APK", "Publish APK to GitHub Release".
- Release `build-1` carries `wake-alarm.apk`; the CI artifact is byte-identical to the locally verified one (SHA-256 `f7281f7c72cad342ad4bcfaae5640a87b814602ec09ca65eb41469fcff347c43`).
- Local `apksigner verify` confirms it is signed by the committed debug key; `aapt2 dump badging` confirms package `com.misaka9981.alarm` and launchable activity `com.misaka9981.alarm.MainActivity`.
- `:core:test` passes (1 test, 0 failures).

Remaining (owner, on the phone): open the stable one-tap URL and install, then confirm it launches to the placeholder screen:

https://github.com/misaka9981/wake-alarm/releases/latest/download/wake-alarm.apk

Ticket 02 is unblocked: the skeleton, the core seam, and the build/release pipeline all exist. The unchecked box is only the physical hand-install, which cannot be performed from CI.

