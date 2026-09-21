# Wake Alarm

A personal Android alarm clock that cannot be silenced by a half-asleep reflex.
The design and vocabulary live in [`CONTEXT.md`](CONTEXT.md) and
[`docs/adr/`](docs/adr/); the MVP spec is in
[`.scratch/wake-alarm-mvp/spec.md`](.scratch/wake-alarm-mvp/spec.md).

## Layout

- `core/` — pure Kotlin, no Android dependencies. Every decidable piece of
  logic lives here. This is the only seam automated tests target.
- `app/` — a thin Android adapter (Jetpack Compose) around `core`.
- `.github/workflows/ci.yml` — runs the `core` JVM unit tests, builds a
  debug-signed APK, and publishes it to a GitHub Release.

## Build and test

```sh
./gradlew :core:test          # the only automated tests
./gradlew :app:assembleDebug  # produces app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and, for the Android build only, the Android SDK.

## Install on the phone

The app is built in the cloud and sideloaded by hand — there is no local
Android toolchain and no `adb`. On the phone browser, open the newest APK from
the Releases page:

```
https://github.com/misaka9981/wake-alarm/releases/latest/download/wake-alarm.apk
```

The debug signature is fixed by a committed debug keystore, so each new build
installs over the previous one.
