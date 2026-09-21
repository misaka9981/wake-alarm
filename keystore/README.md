# Debug keystore

`debug.keystore` is a standard, non-secret Android debug keystore:

- alias: `androiddebugkey`
- store password: `android`
- key password: `android`

It is committed on purpose. CI runs on a fresh runner each time, and a per-run
generated debug key would change on every build, so an APK could not be
installed over a previously installed one. A fixed keystore keeps the debug
signature stable. See ADR-0001.

This file must never be used to sign anything that is released to anyone but
the owner. A release keystore, held in GitHub Secrets, replaces it once the app
stabilises.
