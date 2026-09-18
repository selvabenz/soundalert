# Developer handoff: v0.2.0-beta.1

## Release state

- Version name: `0.2.0-beta.1`
- Version code: `20001`
- Git tag: `v0.2.0-beta.1`
- Phone and Wear OS package ID: `com.bridgeconn.soundalert.v2beta`
- Release channel: public GitHub prerelease

The version name and code are defined once in `gradle.properties` and consumed by both Android modules. The build workflow also reads the version name from that file when naming artifacts.

## Signing

Phone and watch beta APKs use `signing/soundalert-v2-beta.keystore`. The key is intentionally committed because it is a disposable beta-only key with documented credentials and a beta-only application ID. Never reuse it for a production package or Play Store release.

For production, create a private upload/app-signing key, move credentials to encrypted CI secrets, choose the permanent application ID, and ship a separately versioned production build.

## Build and release

The main-branch workflow downloads and verifies YAMNet, runs unit tests, assembles both APKs, checks signatures and 16 KiB zip alignment, enforces the phone APK size ceiling, and uploads separate phone/watch artifacts.

The tag workflow accepts `v0.2.*` tags, verifies that the tag exactly matches `soundAlertVersionName`, repeats tests and APK verification, creates a GitHub prerelease from `RELEASE_NOTES_v0.2.0.md`, and uploads:

- `SoundAlert-v0.2.0-beta.1-PHONE.apk`
- `SoundAlert-v0.2.0-beta.1-WEAR-OS.apk`
- `SoundAlert-v0.2.0-beta.1.sha256`

## Known boundaries

- Detection uses general YAMNet classes and policy filtering; it is not trained on an India-specific dataset.
- Real-device microphone, torch, screen-off, reboot reminder, and Wear OS Data Layer behavior still require the checklist in `docs/TEST_CHECKLIST.md`.
- Android intentionally prevents silent microphone foreground-service startup after reboot; the app posts a resume notification instead.
- The app is a supplementary accessibility aid, not a sole safety-warning system.

## Next developer priorities

1. Run the full device checklist on representative Android phones and at least one Wear OS watch.
2. Record false-positive/false-negative outcomes without retaining user audio unless separately consented.
3. Replace beta signing and the beta application ID before any production-store release.
4. Add instrumented tests for service lifecycle, permission denial, and reboot-resume behavior.
