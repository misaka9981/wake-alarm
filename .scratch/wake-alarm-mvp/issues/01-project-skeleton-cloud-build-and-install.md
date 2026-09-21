# 01: Project skeleton, cloud build, and hand-install

**What to build:** The repository and pipeline exist, and a build reaches the phone. A pushed change produces an installable APK that the owner downloads from the phone browser and opens, launching to a placeholder screen. This establishes the only testing seam the project will use.

**Blocked by:** None (can start immediately)

Status: ready-for-agent

- [ ] The repository is initialised and pushed to a public GitHub repo under the `misaka9981` account, and CI runs on push.
- [ ] A Kotlin + Jetpack Compose app and a pure `core` Kotlin module with no Android dependencies both exist.
- [ ] A trivial `core` unit test exists and passes in CI.
- [ ] CI runs the `core` module's JVM unit tests and builds a debug-signed APK using a committed, non-secret debug keystore.
- [ ] CI publishes the APK to a GitHub Release.
- [ ] The APK installs by hand from the phone browser and launches to a placeholder screen.
