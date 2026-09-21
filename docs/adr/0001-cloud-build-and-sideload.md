# Build the APK in GitHub Actions and sideload it

**Status**: accepted

This is a personal Android app and the development machine has no JDK, Android SDK, or emulator. Rather than install that toolchain locally, we build the APK in GitHub Actions and install it on the device by hand (sideload). The project lives in a public repo so the phone can download Release APKs with a single tap and CI minutes are free.

## Considered Options

- **Local Android SDK** (Android Studio or command-line tools): the conventional path, but the entire toolchain must be installed and maintained on a machine that currently has none.
- **Cloud build + adb** (pull the artifact and install over USB): keeps local setup small while preserving device logs, but still needs platform-tools.
- **Cloud build + sideload only** (chosen): no local Android tooling at all.

## Consequences

- The edit-to-device loop is minutes long, so changes must be batched and verified deliberately.
- CI has no device, so only JVM unit tests run there; platform behaviour is verified by hand on the real phone.
- With no `adb`, there is no `logcat`; the app must carry its own Diagnostic Log (see `CONTEXT.md`).
- A fixed, committed debug keystore is needed during the debug phase, because a fresh CI runner would otherwise generate a new debug key on every run and break in-place upgrades.
