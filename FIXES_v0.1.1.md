# SoundAlert v0.1.1 build fixes

This package repairs the build failures seen in the original v0.1.0 package.

## Fixed

- Removed `org.jetbrains.kotlin.android`, which AGP 9.4 rejects because Kotlin support is built into AGP 9+.
- Added the missing `androidx.activity.compose.setContent` import in `MainActivity.kt`.
- Fixed nullable `Intent?` handling in `SoundDetectionService.onStartCommand()`.
- Updated GitHub Actions from `android-actions/setup-android@v3` to `@v4`.
- Changed the Android platform package to `platforms;android-37.0`.
- Changed Android Build Tools to `37.0.0`.
- Added stack traces to CI test/build steps for clearer failures.
- Bumped the app version to `0.1.1` / versionCode 2.
- Updated the generated APK artifact name to `SoundAlert-v0.1.1-debug.apk`.

## Validation completed in this package environment

- Android XML files parse successfully.
- GitHub Actions YAML parses successfully.
- Required build configuration invariants were checked.
- Platform-independent classifier code compiled with Kotlin and passed smoke tests for silence, horn-like tone, and doorbell-like tone.

The full Android APK must still be compiled on an Android SDK runner. The included GitHub Actions workflow is configured to do that automatically.
