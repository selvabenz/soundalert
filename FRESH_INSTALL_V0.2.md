# SoundAlert v0.2.0-beta.1 — Fresh-install GitHub package

This package intentionally uses a new beta application ID:

`com.bridgeconn.soundalert.v2beta`

That means it can be installed alongside the existing SoundAlert v0.1 app and alongside older
v0.2 debug packages, avoiding Android package/signature conflicts during testing.

## Which APK goes on which device

Install only:

`SoundAlert-v0.2.0-beta.1-INSTALL-ON-PHONE.apk`

on the Android phone.

Do **not** install the Wear OS APK on the phone. The watch APK is:

`SoundAlert-v0.2.0-beta.1-INSTALL-ON-WEAR-OS-WATCH.apk`

and requires a Wear OS watch.

## Stable beta signing

Phone and watch beta APKs are signed with the same stable beta-only keystore included in this
repository. This lets later v0.2 beta GitHub builds update an already-installed v0.2 beta instead
of receiving a new random CI debug certificate on every runner.

The beta keystore is intentionally not a production release key and must never be used for a
Play Store/final production release.

## GitHub Actions

The build workflow verifies each APK with `apksigner` and `zipalign` before publishing artifacts.
It uploads two clearly separated artifacts:

- `INSTALL-ON-PHONE-SoundAlert-v0.2.0-beta.1`
- `INSTALL-ON-WATCH-SoundAlert-v0.2.0-beta.1`
