# SoundAlert for Android

SoundAlert is an Android accessibility prototype intended to help a deaf or hard-of-hearing user notice important environmental sounds through **distinctive vibration patterns** and **large visual alerts**.

Version: **0.1.1**

## Two modes

### Road Alert
- Continuously analyzes microphone audio while the user explicitly enables listening.
- Looks for sustained horn-like low-frequency tonal sounds.
- Haptic pattern: **LONG → short → LONG**.
- Displays `HORN DETECTED` and posts a visual alert notification.

### Home Alert
- Looks for doorbell-like tonal sounds and strong knock-like transients.
- Haptic pattern: **short → short → long pause → short → short**.
- Displays `DOOR SOUND DETECTED` and posts a visual alert notification.

## Privacy

Audio analysis is performed locally in memory. This version:
- does **not** request Internet permission;
- does **not** save microphone recordings;
- does **not** upload microphone audio;
- releases the microphone when the user taps **STOP LISTENING**.

## Safety limitation

**Road Alert is a supplementary awareness aid only.** It is not a certified traffic-safety device and must not be the user's only warning mechanism. Phone microphones can miss horns because of wind, traffic noise, distance, clothing, phone placement, microphone directionality, device processing, or other environmental conditions.

The first detector is intentionally a small offline acoustic heuristic. It is useful for functional testing but should be replaced or augmented by a trained environmental-sound model after collecting representative test recordings from the actual places/devices where the app will be used.

## Technical architecture

- Kotlin
- Jetpack Compose
- Android foreground service with `foregroundServiceType="microphone"`
- `AudioRecord` at 16 kHz mono PCM
- Offline feature extraction / Goertzel tonal analysis
- `VibrationEffect.createWaveform()` custom haptics
- High-priority visual alert notification channel with system vibration disabled (to avoid mixing with the custom pattern)
- No backend and no network permission

The detection logic is isolated in:

`app/src/main/java/com/bridgeconn/soundalert/audio/HeuristicSoundClassifier.kt`

That class can later be replaced with a MediaPipe/TFLite/YAMNet-style model without redesigning the UI, service, or vibration layer.

## Requirements

This project is configured for the Android toolchain current in September 2026:

- Android Studio Quail 4 (2026.1.4) or compatible newer release
- JDK 17+
- compileSdk 37
- targetSdk 37
- minSdk 26 (Android 8.0)
- Android Gradle Plugin 9.4.0
- Gradle 9.6.0
- Compose BOM 2026.09.00

## Gradle build note

The GitHub Actions build does not depend on the Gradle wrapper JAR; it provisions Gradle 9.6.0 directly on the runner. This keeps the repository buildable through GitHub even if the wrapper JAR is not present.

For local command-line builds, either install Gradle 9.6.0 or regenerate the wrapper once with:

```bash
gradle wrapper --gradle-version 9.6.0
```

## Install and test

1. Open the project in Android Studio.
2. Install Android SDK Platform 37 if Android Studio asks for it.
3. Connect an Android phone with USB debugging enabled, or use an emulator for UI testing.
4. Run the app.
5. Tap **START LISTENING** and grant microphone permission.
6. Choose **ROAD** or **HOME**.
7. Use **TEST HORN VIBRATION** / **TEST DOORBELL VIBRATION** first to learn the patterns.
8. Test with real sounds at safe distances and adjust sensitivity.

The debug APK is normally generated at:

`app/build/outputs/apk/debug/app-debug.apk`

## Detector calibration

The current classifier uses a sensitivity setting from 30–95%. Higher sensitivity lowers the detection threshold. A 1–6 second cooldown prevents the same continuous sound from producing rapid repeated alerts.

For publishing or real-world deployment, calibration should include:
- multiple vehicle horn types;
- motorcycles, buses, trucks, auto-rickshaws and cars;
- speech, music, braking, engines and construction noise as negative examples;
- wind and phone-in-pocket tests;
- several Android phone microphones;
- the user's actual home doorbell and common knock patterns.

## Tests included

`HeuristicSoundClassifierTest.kt` includes baseline tests for:
- silence → no alert;
- 440 Hz sustained tone → horn-like alert after consecutive frames;
- 1200 Hz sustained tone → doorbell-like alert after consecutive frames.

The same classifier was compiled with the local Kotlin compiler in the creation environment and the smoke test passed before the ZIP was produced.

## Recommended v0.2

The next serious step is a trained classifier with classes such as:
- vehicle horn;
- emergency siren;
- bicycle bell;
- doorbell;
- knocking;
- speech / music / engine / other noise as explicit negatives.

A smartwatch companion would also be valuable because wrist haptics are much harder to miss than a phone in a pocket or bag.

## Build the APK without Android Studio

This project includes `.github/workflows/build-apk.yml`.

1. Create an empty GitHub repository and upload/push the contents of this folder.
2. Open the repository's **Actions** tab.
3. Select **Build SoundAlert APK**.
4. Choose **Run workflow** (or simply push to `main` / `master`).
5. When the job succeeds, download the artifact named **SoundAlert-v0.1.1-APK**.
6. Unzip the artifact to get `SoundAlert-v0.1.1-debug.apk`.

The workflow installs the required Android API/build tools on the GitHub runner, runs unit tests, builds the debug APK, and uploads it as a downloadable artifact. Android Studio is not required on your computer.
