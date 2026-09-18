# SoundAlert v0.2.0 — Automatic Reliable Alert

SoundAlert is an Android accessibility app for deaf and hard-of-hearing people, including older users who need a very simple interface.

## Normal user interface

v0.2.0 follows one rule:

> **One screen. One switch. One sensitivity control. Everything else automatic.**

There is no scrolling and no mode selector. The activity uses a full-screen immersive canvas. The screen contains:

- a full-screen pulsing background;
- one large ON/OFF control;
- one sensitivity slider.

During an alert the whole background flashes and a large visual symbol is drawn for horn, door or emergency siren.

### Hidden one-time option

Long-press the large ON/OFF control for one second to toggle optional flashlight alerts. The first time flash is enabled, Android asks for camera permission because torch control requires it. A small lightning mark appears when flash is enabled. This keeps the normal screen uncluttered.

## Detection engine

The GitHub build downloads the verified 4.13 MB YAMNet TFLite environmental-sound model and packages it inside the phone APK. MediaPipe Tasks Audio 1.0.0 performs all classification on-device.

SoundAlert adds its own policy on top of the model:

- horn group: vehicle/car horn/honking, toot, truck/air horn, bicycle bell;
- emergency group: police, ambulance, fire-engine and generic siren classes;
- home group: doorbell, ding-dong and knock;
- 2-of-3 frame confirmation;
- explicit negative/background suppression;
- automatic ROAD/HOME/UNCERTAIN threshold bias;
- automatic numerical background calibration per environment;
- full detector reset after the vibration completes and two quiet frames are seen.

## Negative/background filtering

The explicit negative set includes:

- speech, conversation, shouting and crowds;
- music and radio;
- engine/idling/revving;
- wind and microphone wind noise;
- construction tools;
- traffic without horns;
- television;
- dogs;
- household noise and appliances;
- generic environmental/noise classes.

These sounds do not directly trigger an alert and can suppress weak positive detections.

## Automatic context

Once the user presses ON, SoundAlert keeps one microphone foreground service running and automatically adjusts its detection profile using:

- YAMNet environment/traffic/indoor labels;
- low-rate accelerometer motion;
- learned background levels.

No GPS or location permission is used.

Context only changes thresholds; it never completely disables horn or door detection.

## Screen-locked/background reliability

- Android `microphone` foreground service
- partial CPU wake lock while SoundAlert is ON
- persistent foreground notification
- activity can be closed and screen can lock while listening continues

Modern Android does not allow an app to silently start a microphone foreground service from the background. Therefore, after a reboot SoundAlert posts a small resume reminder if it was previously ON. Opening the app resumes listening while the activity is visible.

## Haptic vocabulary

- **Horn:** LONG — short — LONG
- **Door:** short short — pause — short short
- **Emergency siren:** triple-long — pause — triple-long

The detector is reset after each vibration before another alert can fire.

## Wear OS

The project includes a separate `wear` companion module. When a compatible Wear OS watch has the companion APK installed, phone alerts are sent automatically through the Wearable Data Layer and reproduced with the same distinctive wrist vibration pattern.

The phone app works normally without a watch.

## Privacy

- no microphone recordings are saved;
- no audio is uploaded;
- no GPS/location permission;
- classification happens on-device;
- calibration stores only numerical background baselines.

## Indian traffic scope

v0.2.0 recognizes the generic horn classes that cover the acoustic events produced by cars, motorcycles/scooters, auto-rickshaws, buses and trucks, including truck/air horns. It does **not** claim to identify the vehicle type from the horn.

For substantially better India-specific accuracy, the next model-tuning step is to collect representative positive and negative recordings and train/test a compact SoundAlert-specific model. See `docs/REAL_WORLD_CALIBRATION.md`.

## Build without Android Studio

The repository includes GitHub Actions.

### Normal build

Push the project to GitHub, then open:

`Actions → Build SoundAlert v0.2 APKs`

The workflow:

1. installs JDK 17, Android API 37.0 and Gradle 9.6.0;
2. downloads YAMNet and verifies its SHA-256;
3. runs unit tests;
4. builds phone and Wear OS APKs;
5. reports phone APK size;
6. uploads both APKs plus checksums as an artifact.

Expected files:

- `SoundAlert-v0.2.0-phone.apk`
- `SoundAlert-v0.2.0-wear.apk`
- `SoundAlert-v0.2.0.sha256`

### Automatic GitHub Release assets

Pushing a tag such as:

```bash
git tag v0.2.0
git push origin v0.2.0
```

runs `.github/workflows/release-apk.yml`, creates/updates the GitHub Release and attaches the APK files directly under **Release → Assets**.

## Important signing note

The current CI package builds debug-signed prototype APKs. The phone and watch APKs produced in the **same workflow run** share the same package/signing identity and can communicate through Wear OS Data Layer.

Before distributing SoundAlert as a long-lived public production app, configure a persistent private release signing key. Because earlier v0.1 debug builds were created on ephemeral GitHub runners, users may need to uninstall an older prototype before installing this v0.2 prototype if Android reports a signature mismatch.

## Size strategy

v0.2.0 removes Jetpack Compose and uses one custom Android View specifically to keep the phone APK small. CI warns above 25 MiB and fails above 40 MiB. After the first GitHub build, use the reported APK size to decide whether MediaPipe/ABI trimming is needed before public release.

## Safety

Road alerts are supplementary awareness aids. Do not treat SoundAlert as the sole traffic-warning system. Phone microphones and classifiers can miss or misclassify sounds because of distance, wind, traffic, phone placement, clothing, hardware differences and other environmental factors.
