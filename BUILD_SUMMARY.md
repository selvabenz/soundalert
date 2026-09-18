# SoundAlert v0.2.0 build summary

## Implemented

- Fixed one-screen, non-scrollable phone UI with no Compose dependency.
- Large ON/OFF control and sensitivity slider only in the normal interface.
- Full-screen pulsing visual state and large horn/door/siren pictograms.
- Offline YAMNet + MediaPipe audio classification.
- Vehicle/car horn, toot and truck/air-horn detection.
- Doorbell, ding-dong and knock detection.
- Emergency siren detection.
- Explicit negative/background filtering for speech, music, engines, wind, construction, non-horn traffic, TV, dogs and household noise.
- Two-of-three-frame confirmation and duplicate suppression.
- Detector reset after vibration plus quiet-frame re-arm.
- Automatic ROAD/HOME/UNCERTAIN threshold adaptation using sound context + low-rate accelerometer; no GPS permission.
- Automatic numerical background calibration without storing audio.
- Foreground microphone service + partial wake lock for screen-locked listening.
- Optional flashlight alert, with camera permission requested only when the user enables it.
- Wear OS companion module using Data Layer messages and matching haptic patterns.
- GitHub Actions for tests, APK build, checksums and automatic Release assets.
- Phone APK size guardrails in CI.

## Validation performed in this package

- Android manifest XML parsed successfully.
- Wear manifest XML parsed successfully.
- GitHub Actions YAML parsed successfully.
- Shell scripts passed `bash -n`.
- Pure Kotlin detection-policy/context smoke tests passed for horn, doorbell, speech/music suppression, non-horn traffic suppression and 2-of-3 confirmation.
- Static scan confirms there are no Jetpack Compose references.

## Build boundary

This chat runtime does not contain the Android SDK, so the complete Android APK cannot be compiled locally here. The included GitHub Actions workflow installs the Android toolchain, downloads and verifies the YAMNet model, runs unit tests and builds both APKs.

## Field-testing boundary

The detector uses general YAMNet classes plus SoundAlert-specific filtering. It has not yet been trained on a dedicated India-only horn dataset. Real-world testing with Indian cars, motorcycles/scooters, auto-rickshaws, buses and trucks is required before treating v0.2.0 as calibrated for daily safety use.
